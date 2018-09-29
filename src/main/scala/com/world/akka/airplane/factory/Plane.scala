package com.world.akka.airplane.factory

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.world.akka.airplane.EventReporter_1.ProductionEventSource.RegisterListener
import com.world.akka.airplane.actors._
import com.world.akka.airplane.actors.Altimeter.AltitudeUpdate
import com.world.akka.airplane.supervisor.IsolatedLifeCycleSupervisor.WaitForStart
import com.world.akka.airplane.supervisor.{IsolatedResumeSupervisor, IsolatedStopSupervisor, OneForOneStrategyFactory}
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._

object Plane {
  // Returns the control surface to the Actor that
  // asks for them
  case object GiveMeControl
  case class Controls(controls: ActorRef)

}
// We want the Plane to own the Altimeter and we're going to
// do that by passing in a specific factory we can use to
// build the Altimeter
class Plane extends Actor with ActorLogging {
  this: LeadFlightAttendantProvider with AltimeterProvider with PilotProvider =>
  implicit val askTimeout = Timeout(1.second)
  import Plane._
  //to create a supervised child actor from within an actor
  //Using context.actorOf creates the ActorRef and ties it to the current actor as a child
  //val altimeter: ActorRef = context.actorOf(Props[Altimeter], "Altimeter")
  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config
  //commented out as a part of refactoring to get instances from Traits
//  val altimeter: ActorRef = context.actorOf(Props(newAltimeter), "Altimeter")
//  val controls: ActorRef = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
//  val pilot = context.actorOf(Props(newPilot), config.getString(s"$cfgstr.pilotName"))
//  val copilot = context.actorOf(Props(newCopilot), config.getString(s"$cfgstr.copilotName"))
//  val autopilot = context.actorOf(Props(newAutopilot), "Autopilot")
//  val flightAttendant = context.actorOf(Props(newLeadFlightAttendant), config.getString(s"$cfgstr.leadAttendantName"))

  override def preStart() {
    // Register ourself with the Altimeter to receive updates
    // on our altitude
    context.child("altimeter").get ! RegisterListener(self)
    List(context.child("pilot").get, context.child("copilot").get) foreach { _ ! Pilot.ReadyToGo }
  }

  def receive: PartialFunction[Any, Unit] = {
    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now: $altitude"
    case GiveMeControl =>
      log info "Plane giving control."
      //Bad idea to directly send ActorRef: wrap it in the case class instead
      /*sender ! controls*/
      sender! Controls(context.child("ControlSurfaces").get)
  }

  def startEquipment() {
    val controls = context.actorOf(Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          context.actorOf(Props(newAutopilot), "Autopilot")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
      }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

//  def startPeople() {
//    val people = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
//        def childStarter() {
//          // These children get implicitly added to the hierarchy
//          context.actorOf(Props(new Pilot), config.getString(s"$cfgstr.pilotName"))
//          context.actorOf(Props(new Copilot), config.getString(s"$cfgstr.copilotName"))
//        }
//      }), "Pilots")
//    // Use the default strategy here, which restarts indefinitely
//    context.actorOf(Props(newLeadFlightAttendant), config.getString(s"$cfgstr.leadAttendantName"))
//    Await.result(people ? WaitForStart, 1.second)
//  }
}
