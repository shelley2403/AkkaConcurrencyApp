package com.world.akka

import akka.actor.{Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestKit}
import com.world.akka.airplane.EventReporter_1.ProductionEventSource
import com.world.akka.airplane.EventReporter_1.ProductionEventSource.{RegisterListener, UnregisterListener}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

//Taking TestEventSource instead of testing ProductionEventSource probably cause we cant flush messages to the real Event Source
// We can't test a "trait" very easily, so we're going to
// create a specific EventSource derivation that conforms to
// the requirements of the trait so that we can test the
// production code.

///Since TestEventSource is an Actor, it can implement ProductionEventSource which is self typed Actor
class TestEventSource extends Actor with ProductionEventSource {
  def receive = eventSourceReceive
}

class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec")) with WordSpecLike with Matchers with BeforeAndAfterAll{
  override def afterAll() { system.terminate() }
  "EventSource" should {
    "allow us to register a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor

      real.receive(RegisterListener(testActor))
      real.listeners should contain(testActor)
    }

    "allow us to unregister a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.receive(UnregisterListener(testActor))
      real.listeners.size should be (0)
    }

    "send the event to our test actor" in {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibonacci")
      expectMsg("Fibonacci")
    }
  }

}
