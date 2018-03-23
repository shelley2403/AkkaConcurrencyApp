package com.world.akka.airplane.factory

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.world.akka.airplane.Altimeter.AltitudeUpdate
import com.world.akka.airplane.EventReporter_1.ProductionEventSource.RegisterListener
import com.world.akka.airplane.{Altimeter, ControlSurfaces}

object Plane {

  // Returns the control surface to the Actor that
  // asks for them
  case object GiveMeControl

}

// We want the Plane to own the Altimeter and we're going to
// do that by passing in a specific factory we can use to
// build the Altimeter
class Plane extends Actor with ActorLogging {

  import Plane._

  //to create a supervised child actor from within an actor
  //Using context.actorOf creates the ActorRef and ties it to the current actor as a child
  //val altimeter: ActorRef = context.actorOf(Props[Altimeter], "Altimeter")
  val altimeter: ActorRef = context.actorOf(Props(Altimeter()), "Altimeter")

  val controls: ActorRef = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")

  override def preStart() {
    altimeter ! RegisterListener(self)
  }

  def receive: PartialFunction[Any, Unit] = {
    case AltitudeUpdate(altitude) =>
      log info s"Altitude is now: $altitude"
    case GiveMeControl =>
      log info "Plane giving control."
      sender ! controls
  }
}
