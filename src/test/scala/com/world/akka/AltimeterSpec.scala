package com.world.akka

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestLatch}
import com.world.akka.airplane.Altimeter.{AltitudeUpdate, RateChange}
import com.world.akka.airplane.EventReporter_1.EventSource_1
import com.world.akka.airplane.EventReporting.EventSource
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.Await

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  import com.world.akka.airplane.actors.Altimeter

  override def afterAll() { system.terminate() }

  // We'll instantiate a Helper class for every test, making
  // things nicely reusable.
  class Helper {
    object EventSourceSpy {
      // The latch gives us fast feedback when
      // something happens
      val latch = TestLatch(1)
    }
    // Our special derivation of EventSource gives us the
    // hooks into concurrency
    trait EventSourceSpy extends EventSource_1 {
      override def sendEvent[T](event: T): Unit =
        EventSourceSpy.latch.countDown()
      // We don't care about processing the messages that
      // EventSource usually processes so we simply don't
      // worry about them.
      override def eventSourceReceive = Actor.emptyBehavior
    }

    // The slicedAltimeter constructs our Altimeter with
    // the EventSourceSpy
    def slicedAltimeter = new Altimeter with EventSourceSpy

    // This is a helper method that will give us an ActorRef
    // and our plain ol' Altimeter that we can work with
    // directly.
    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }

  "Altimeter" should {
    "record rate of climb changes" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(1f))
      real.rateOfClimb should be (real.maxRateOfClimb)
    }
    "keep rate of climb changes within bounds" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(2f))
      real.rateOfClimb should be (real.maxRateOfClimb)
    }
    "calculate altitude changes" in new Helper {
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)

      //Simple case like RegisterListener(testActor) RateChange(1f) can be tested with ease
      //For testing cases like Tick RegisterListener(testActor) Tick Tick RateChange(1f) Tick comes in FishForMessage
      //The fishForMessage call will run the passed-in partial function repeatedly so long as it returns false AND up until the (default) timeout
      //Once it returns true, the test passes and thus we don't need sleep in our test case to allow that condition to happen, THUS SAVING TIME
      fishForMessage() {
        case AltitudeUpdate(altitude) if altitude == 0f =>
          false
        case AltitudeUpdate(altitude) =>
          true
      }
    }
    "send events" in new Helper {
      val (ref, _) = actor()
      Await.ready(EventSourceSpy.latch, 1.second)
      EventSourceSpy.latch.isOpen should be (true)
    }
  }

}
