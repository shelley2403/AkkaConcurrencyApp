package com.world.akka.airplane.actors

import akka.actor.{Actor, ActorRef, RootActorPath}
import com.world.akka.airplane.factory.Plane.{Controls, GiveMeControl}

object Pilots {

  case object ReadyToGo

  case object RelinquishControl

  case object Controls

}

class Pilot extends Actor {
  import Pilots._

  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      copilot = context.child("../" + copilotName).get
      autopilot = context.child("../Autopilot").get
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

class Copilot extends Actor {

  import Pilots._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.child("../" + pilotName).get
      autopilot = context.child("../Autopilot").get
  }
}

class AutoPilot extends Actor {
  override def receive = Actor.emptyBehavior
}