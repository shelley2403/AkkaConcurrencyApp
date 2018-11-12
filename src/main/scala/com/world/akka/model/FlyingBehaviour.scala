package com.world.akka.model

import akka.actor.{Actor, ActorRef, FSM, Props}
import com.world.akka.airplane.EventReporter_1.ProductionEventSource.{RegisterListener, UnregisterListener}
import com.world.akka.airplane.actors.ControlSurfaces.{StickBack, StickForward, StickLeft, StickRight}
import scala.concurrent.duration._

trait FlyingProvider {
  //Including StaticDependancy
  def newFlyingBehaviour(plane: ActorRef,
                         heading: ActorRef,
                         altimeter: ActorRef): Props = Props(new FlyingBehaviour(plane, heading, altimeter))
}

object FlyingBehaviour {

  // The states governing behavioural transitions
  sealed trait State

  case object Idle extends State

  case object Flying extends State

  case object PreparingToFly extends State

  // Someone can tell the FlyingBehaviour to fly
  case class Fly(target: CourseTarget)

  // Helper classes to hold course data
  case class CourseTarget(altitude: Double, heading: Float, byMillis: Long)

  case class CourseStatus(altitude: Double, heading: Float, headingSinceMS: Long, altitudeSinceMS: Long)

  // We're going to allow the FSM to vary the behaviour that calculates the
  // control changes using this function definition
  type Calculator = (CourseTarget, CourseStatus) => Any

  // The Data that our FlyingBehaviour can hold
  sealed trait Data

  //Initial Data
  case object Uninitialized extends Data

  // This is the 'real' data.  We're going to stay entirely immutable and, in doing so, we're going to encapsulate
  // all of the changing state data inside this class
  case class FlightData(controls: ActorRef, elevCalc: Calculator, bankCalc: Calculator, target: CourseTarget, status: CourseStatus) extends Data

  def currentMS = System.currentTimeMillis

  // Calculates the amount of elevator change we need to make and returns it
  def calcElevator(target: CourseTarget, status: CourseStatus): Any = {
    val alt = (target.altitude - status.altitude).toFloat
    val dur = target.byMillis - status.altitudeSinceMS
    if (alt < 0) StickForward((alt / dur) * -1)
    else StickBack(alt / dur)
  }

  // Calculates the amount of bank change we need to make and returns it
  def calcAilerons(target: CourseTarget, status: CourseStatus): Any = {
    import scala.math.{abs, signum}
    val diff = target.heading - status.heading
    val dur = target.byMillis - status.headingSinceMS
    val amount = if (abs(diff) < 180) diff
    else signum(diff) * (abs(diff) - 360f)
    if (amount > 0) StickRight(amount / dur)
    else StickLeft((amount / dur) * -1)
  }

  // Let people change the calculation functions
  case class NewElevatorCalculator(f: Calculator)
  case class NewBankCalculator(f: Calculator)
}

//The class mixes in the FSM, which requires type parameters for the state and data
class FlyingBehaviour(plane: ActorRef, heading: ActorRef, altimeter: ActorRef) extends Actor with FSM[FlyingBehaviour.State, FlyingBehaviour.Data] {

  import FlyingBehaviour._
  import com.world.akka.airplane.actors.RefactoredPlane._
  import com.world.akka.airplane.actors.Altimeter._
  import zzz.akka.avionics.HeadingIndicator._

  //use to tell the FlyingBehaviour that we need to adjust the plane's altitude and heading.
  case object Adjust

  // Sets up the initial values for state and data in the FSM
  startWith(Idle, Uninitialized)

  def adjust(flightData: FlightData): FlightData = {
    val FlightData(c, elevCalc, bankCalc, t, s) = flightData
    c ! elevCalc(t, s)
    c ! bankCalc(t, s)
    flightData
  }

  def prepComplete(data: Data): Boolean = {
    data match {
      case FlightData(c, _, _, _, s) =>
        if (c != context.system.deadLetters && s.heading != -1f && s.altitude != -1f)
          true
        else
          false
      case _ =>
        false
    }
  }

  //When calls help us to define behaviors of particular states
  //when replaces receive method
  when(Idle) {
    case Event(Fly(target), _) =>
      goto(PreparingToFly) using FlightData(
        context.system.deadLetters,
        calcElevator,
        calcAilerons,
        target,
        CourseStatus(-1, -1, 0, 0)
      )
  }

  //Preparing to fly needs: controls, current height, and current heading
  //Our FSM doesn't have any of this information in the idle state, and it's going to need it before it can transition
  //to the state where it's flying. We start by sending out some requests when transitioning to the PreparingToFly state
  onTransition {
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }

  //When all the data has been collected form the received messages, we can prepare FlightData and fly the plane
  //Data can come in any order

  //HeadingUpdate, which lets it know the current heading
  //AltitudeUpdate, which lets it know the current altitude
  //Controls, containing the control surfaces that it will use to actually fly the plane

  //transform: Transforms the results of previous handler based on whether the data is available or not
  when (PreparingToFly, stateTimeout = 5.seconds)(transform {
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(heading = head,
          headingSinceMS = currentMS))
    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(altitude = alt,
          altitudeSinceMS = currentMS))
    case Event(Controls(ctrls), d: FlightData) =>
      stay using d.copy(controls = ctrls)
    case Event(StateTimeout, _) =>
      plane ! LostControl
      goto (Idle)
  } using {
    //If prepComplete indicates that we're good, we can transform the state information, indicating a new destination state (Flying).
    // If prepComplete indicates otherwise, we don't do anything at all, and the result of the main block is used instead.
    case s if prepComplete(s.stateData) =>
      s.copy(stateName = Flying)
  })

  //give the FlyingBehaviour time slices for which it can recalculate and adjust the controls of the plane.
  onTransition {
    case PreparingToFly -> Flying =>
      setTimer("Adjustment", Adjust, 200.milliseconds,
        repeat = true)
  }

  //The altimeter tells us the current altitude.
  // The heading indicator tells us the current heading.
  // The internal timer tells us that it's time once again to adjust the plane's direction.
  when(Flying) {
    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(altitude = alt,
          altitudeSinceMS = currentMS))
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(status =
        d.status.copy(heading = head,
          headingSinceMS = currentMS))
    case Event(NewBankCalculator(f), d: FlightData) =>
      stay using d.copy(bankCalc = f)
    case Event(NewElevatorCalculator(f), d: FlightData) =>
      stay using d.copy(elevCalc = f)
    case Event(Adjust, flightData: FlightData) =>
      stay using adjust(flightData)
  }

  onTransition {
    case Flying -> _ =>
      cancelTimer("Adjustment")
  }

  onTransition {
    case _ -> Idle =>
      heading ! UnregisterListener(self)
      altimeter ! UnregisterListener(self)
  }

  initialize

}
