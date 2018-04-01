package com.world.akka

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import com.world.akka.airplane.actors.{AttendantResponsiveness, FlightAttendant}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers

object TestFlightAttendant {
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 1
  }
}

class FlightAttendantSpec extends TestKit(ActorSystem("FlightAttendantSpec", ConfigFactory.parseString(
  "akka.scheduler.tick-duration = 1ms"))) with ImplicitSender with WordSpecLike with Matchers {
  import FlightAttendant._

  "FlightAttendant" should {
    "get a drink when asked" in {
      val a = TestActorRef(Props(TestFlightAttendant()))
      a ! GetDrink("Soda")
      expectMsg(Drink("Soda"))
    }
  }
}
