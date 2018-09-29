package com.world.akka.airplane.actors

import akka.actor.{Actor, ActorRef}
import com.world.akka.airplane.actors.Pilot.ReadyToGo
import com.world.akka.airplane.factory.RefactoredPlane.{Controls, GiveMeControl}

object Pilot {
  case object ReadyToGo
  case object RelinquishControl
  case object Controls
}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor {


  //var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
//  var autopilot: ActorRef = context.system.deadLetters
  val copilotName: String = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

  def receive: PartialFunction[Any, Unit] = {
    case ReadyToGo =>
      //context.parent ! GiveMeControl
      plane ! GiveMeControl
      copilot = context.child("../" + copilotName).get
      //autopilot = context.child("../Autopilot").get
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

class Copilot(plane: ActorRef, autoPilot: ActorRef, altimeter:ActorRef) extends Actor {

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

