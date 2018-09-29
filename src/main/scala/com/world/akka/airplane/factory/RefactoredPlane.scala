package com.world.akka.airplane.factory

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.world.akka.airplane.actors.{AltimeterProvider, ControlSurfaces, LeadFlightAttendantProvider, Pilot}
import akka.pattern.ask
import com.typesafe.config.Config
import com.world.akka.airplane.EventReporter_1.ProductionEventSource.RegisterListener
import com.world.akka.airplane.actors.Altimeter.AltitudeUpdate
import com.world.akka.airplane.actors.Pilot.ReadyToGo
import com.world.akka.airplane.supervisor.IsolatedLifeCycleSupervisor.WaitForStart
import com.world.akka.airplane.supervisor.{IsolatedResumeSupervisor, IsolatedStopSupervisor, OneForOneStrategyFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

object RefactoredPlane {
  case object GiveMeControl
  case class Controls(controls: ActorRef)
}

class RefactoredPlane extends Actor with ActorLogging {
  this: LeadFlightAttendantProvider with AltimeterProvider with PilotProvider =>
  implicit val askTimeout: Timeout = Timeout(1.second)
  import Plane._
  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config: Config = context.system.settings.config
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")
  val leadAttendantName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.leadAttendantName")

  override def preStart() {
    startEquipment()
    startPeople()
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive: PartialFunction[Any, Unit] = {
    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now: $altitude"
    case GiveMeControl =>
      log info "Plane giving control."
      sender! Controls(context.child("ControlSurfaces").get)
  }

  def actorForControls(name: String): ActorRef =
    context.child("Equipment/" + name).get

  def actorForPilots(name: String) =
    context.child("Pilots/" + name).get

  def startPeople() {
    val plane = self
    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter)), copilotName)
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
        }
      }), "Pilots")
    // Use the default strategy here, which
    // restarts indefinitely
    context.actorOf(Props(newLeadFlightAttendant),  leadAttendantName)
    Await.result(people ? WaitForStart, 1.second)
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

}
