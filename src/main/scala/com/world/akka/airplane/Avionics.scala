package com.world.akka.airplane

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.world.akka.airplane.actors.ControlSurfaces.StickBack
import akka.pattern._
import com.world.akka.airplane.actors.RefactoredPlane

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

// The futures created by the ask syntax need an
// execution context on which to run, and we will use the
// default global instance for that context

object Avionics {
  // needed for '?' below
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props[RefactoredPlane], "Plane")

  def main(args: Array[String]) {
    // Grab the control
    val control = Await.result((plane ? RefactoredPlane.GiveMeControl).mapTo[ActorRef], 5.seconds)
    //the type that is returned as an Any, due to the fact that messages between actors are of type Any
    // Takeoff!
    system.scheduler.scheduleOnce(200.millis) {
      control ! StickBack(1f)
    }
    // Level out
    system.scheduler.scheduleOnce(1.seconds) {
      control ! StickBack(0f)
    }
    // Climb
    system.scheduler.scheduleOnce(3.seconds) {
      control ! StickBack(0.5f)
    }
    // Level out
    system.scheduler.scheduleOnce(4.seconds) {
      control ! StickBack(0f)

    }
    // Shut down
    system.scheduler.scheduleOnce(5.seconds) {
      system.terminate()
    }
  }
}
