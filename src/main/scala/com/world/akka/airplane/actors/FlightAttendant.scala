package com.world.akka.airplane.actors

import akka.actor.Actor
import scala.concurrent.duration._

trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration =
    scala.util.Random.nextInt(maxResponseTimeMS).millis
}

object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)
  // By default we will make attendants that respond
  // within 5 minutes
  def apply() = new FlightAttendant
    with AttendantResponsiveness  {
    val maxResponseTimeMS = 300000
  }
}

class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>
  //FlightAttendant actor self-types to AttendantResponsiveness
  //Scala will look at the companion object to define it
  //Commenting the trait from the object will give compile error stating the object doesn't self-type to AttendantResponsiveness
  import FlightAttendant._
  // bring the execution context into implicit scope for the
  // scheduler below
  implicit val ec = context.dispatcher
  def receive = {
    case GetDrink(drinkname) =>
      // We don't respond right away, but use the scheduler to
      // ensure we do eventually
      context.system.scheduler.scheduleOnce(
        responseDuration, sender, Drink(drinkname))
  }
}
