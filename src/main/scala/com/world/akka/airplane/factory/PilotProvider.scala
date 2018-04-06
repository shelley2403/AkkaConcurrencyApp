package com.world.akka.airplane.factory

import akka.actor.Actor
import com.world.akka.airplane.actors.{AutoPilot, Copilot, Pilot}

trait PilotProvider {
  def newPilot: Actor = new Pilot
  def newCopilot: Actor = new Copilot
  def newAutopilot: Actor = new AutoPilot
}
