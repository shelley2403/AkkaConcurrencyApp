package com.world.akka

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestKit
import com.world.akka.basics.MyActor
import org.scalatest._
import scala.concurrent.Await

class TestKitSpec(actorSystem: ActorSystem) extends TestKit(actorSystem) with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach

class MyActorSpec extends TestKitSpec (ActorSystem("MyActorSpec")) /*with ParallelTestExecution*/ {
  override def afterAll() { system.terminate() }
  def makeActor: ActorRef = system.actorOf(Props[MyActor], "MyActor")

  "My Actor" should {
    "throw if constructed with the wrong name" in {
        // use a generated name
        val a = system.actorOf(Props[MyActor])
      }
    "construct without exception" in {
      val a = makeActor
      // The throw will cause the test to fail
    }
    "respond with a Pong to a Ping" in {
      val a = makeActor
      a ! "Hello"
//      expectMsg("Hi")
    }
  }

  //Run the test sequentially to give them a safe environment to run safely and not run with ParallelTestExecution
  //Add BeforeAndAfterEach and remove the parallelism and afterEach
  //between each test we could shut down the actor that we want to recreate. This doesn't work, though, because the stop function is asynchronous; the next test will start way before the stop has had time to complete
  override def afterEach() {
    system.stop(makeActor)
  }
}
