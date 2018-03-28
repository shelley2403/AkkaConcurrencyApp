package com.world.akka

import akka.actor.{ActorRef, ActorSystem, Props}
import com.world.akka.basics.MyActor
import org.scalatest.{Matchers, ParallelTestExecution, fixture}

//True isolated test running in parallel
// The ActorSys is a fine spot for definition of a shared fixture.  For example, instead of having mutable data at the spec level, you could put it in the ActorSys and let the constructor initialize the data appropriately.
class MyNewActorSpec extends fixture.WordSpec with fixture.UnitFixture with Matchers with ParallelTestExecution {
  def makeActor(system: ActorSystem): ActorRef = system.actorOf(Props[MyActor], "MyActor")

  "My Actor" should {
    "throw when made with the wrong name" in new ActorSys {
        // use a generated name
        val a = system.actorOf(Props[MyActor])

    }
    "construct without exception" in new ActorSys {
      val a = makeActor(system)
      // The throw will cause the test to fail
    }
    "respond with a Pong to a Ping" in new ActorSys {
      val a = makeActor(system)
      a ! "Hi"
    }
  }

}
