package com.world.akka.airplane.EventReporter_1

import akka.actor.{Actor, ActorRef}

trait EventSource_1 {
  def sendEvent[T](event: T): Unit
  def eventSourceReceive: Actor.Receive
}

object ProductionEventSource {
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

//Restricting this trait to only be implemented by Actors
trait ProductionEventSource extends EventSource_1 {
  this: Actor =>
  import ProductionEventSource._
  var listeners = Vector.empty[ActorRef]
  def sendEvent[T](event: T): Unit = listeners foreach {
    _ ! event
  }
  def eventSourceReceive: Receive = {
    case RegisterListener(listener) =>
      listeners = listeners :+ listener
    case UnregisterListener(listener) =>
      listeners = listeners filter { _ != listener }
  }
}