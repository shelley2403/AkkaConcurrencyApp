package com.world.akka.airplane.factory

import akka.actor.{Actor, ActorRef}
import com.world.akka.airplane.actors.{AutoPilot, Copilot, Pilot}

//CakePattern
trait PilotProvider {
  //Introducing DI
  def newPilot(plane: ActorRef, autoPilot: ActorRef, controls: ActorRef, altimeter:ActorRef): Actor = new Pilot(plane, autoPilot, controls, altimeter)
  def newCopilot(plane: ActorRef, autoPilot: ActorRef, altimeter:ActorRef): Actor = new Copilot(plane, autoPilot, altimeter)
  def newAutopilot: Actor = new AutoPilot
}
