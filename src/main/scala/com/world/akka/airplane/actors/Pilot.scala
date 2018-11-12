package com.world.akka.airplane.actors

import akka.actor.FSM.SubscribeTransitionCallBack
import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.world.akka.airplane.actors.ControlSurfaces.{StickBack, StickForward, StickLeft, StickRight}
import com.world.akka.airplane.actors.Pilot.ReadyToGo
import com.world.akka.airplane.actors.RefactoredPlane.{Controls, GiveMeControl}
import com.world.akka.model.FlyingBehaviour._

import scala.concurrent.duration._
import akka.pattern.ask
import com.world.akka.model.DrinkingBehaviour.{FeelingLikeZaphod, FeelingSober, FeelingTipsy}
import com.world.akka.model.{DrinkingProvider, FlyingProvider}

object Pilot {

  case object ReadyToGo

  case object RelinquishControl

  case object Controls

  // Calculates the elevator changes when we're a bit tipsy
  val tipsyCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }
  // Calculates the aileron changes when we're a bit tipsy
  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickLeft(amt * 1.03f)
      case StickRight(amt) => StickRight(amt * 1.03f)
      case m => m
    }
  }

  // Calculates the elevator changes when we're totally out of it
  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickBack(1f)
      case StickBack(amt) => StickForward(1f)
      case m => m
    }
  }
  // Calculates the aileron changes when we're totally out of it
  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickRight(1f)
      case StickRight(amt) => StickLeft(1f)
      case m => m
    }
  }
}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef, heading: ActorRef) extends Actor {
  this: DrinkingProvider with FlyingProvider =>

  import Pilot._

  implicit val askTimeout = Timeout(1.second)
  //var controls: ActorRef = context.system.deadLetters
  //  var autopilot: ActorRef = context.system.deadLetters
  val copilotName: String = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

  def setCourse(flyer: ActorRef) {
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
  }

  override def preStart() {
    // Create our children
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading, altimeter), "FlyingBehaviour")
  }

  // We've pulled the bootstrapping code out into a separate
  // receive method.  We'll only ever be in this state once,
  // so there's no point in having it around for long
  def bootstrap: Receive = {
    case ReadyToGo =>
      val coPilot = context.actorSelection("../" + copilotName)
      val flyer = context.actorSelection("FlyingBehaviour").resolveOne()
      for {
        flyerRef <- flyer
        coPilotRef <- coPilot
      } yield {
        flyerRef ! SubscribeTransitionCallBack(self)
        setCourse(flyerRef)
        context.become(sober(coPilotRef, flyerRef))
      }
  }

  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => // We're already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  // The 'tipsy' behaviour
  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => // We're already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  // The 'zaphod' behaviour
  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => // We're already Zaphod
  }

  // The 'idle' state is merely the state where the Pilot
  // does nothing at all
  def idle: Receive = {
    case _ =>
  }

  // Updates the FlyingBehaviour with sober calculations and then becomes the sober behaviour
  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }

  // Updates the FlyingBehaviour with tipsy calculations and then becomes the tipsy behaviour
  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }

  // Updates the FlyingBehaviour with zaphod calculations and then becomes the zaphod behaviour
  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  // Initially we start in the bootstrap state
  def receive: Receive = bootstrap
}

//  def receive: PartialFunction[Any, Unit] = {
//    case ReadyToGo =>
//      //context.parent ! GiveMeControl
//      plane ! GiveMeControl
//      copilot = context.child("../" + copilotName).get
//      //autopilot = context.child("../Autopilot").get
//    case Controls(controlSurfaces) =>
//      controls = controlSurfaces
//  }

class Copilot(plane: ActorRef, autoPilot: ActorRef, altimeter: ActorRef) extends Actor {

  import Pilot._

  var controls: ActorRef = context.system.deadLetters
  //var pilot: ActorRef = context.system.deadLetters
  //var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
    //pilot = context.child("../" + pilotName).get
    //autopilot = context.child("../Autopilot").get
  }
}

