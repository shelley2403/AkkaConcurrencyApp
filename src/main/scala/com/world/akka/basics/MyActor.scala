package com.world.akka.basics

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props, Terminated}
import scala.concurrent.duration._

case class Gamma(g: String)
case class Beta(b: String, g: Gamma)
case class Alpha(b1: Beta, b2: Beta)

class MyActor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(5, 1 minute) {
      case _ => Restart
    }

  // Watch any CHILD Actor using Actor contextOf and get Terminated msg if it dies along with dead child actor's ActorRef
  override def preStart() {
    context.watch(context.actorOf(Props[SomeOtherActor]))
  }

  def receive1: Receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
  }

  def receive2: Receive = {
    // Literal String match
    case "Hello" =>
      println("Hi")
      sender ! "Hi"
    // Literal Int match
    case 42 =>
      println("I don't know the question." +
        "Go ask the Earth Mark II.")

    // Matches any string at all
    case s: String =>
      println(s"You sent me a string: $s")

    // Match a more complex case class structure
    case Alpha(Beta(b1, Gamma(g1)), Beta(b2, Gamma(g2))) =>
      println(s"beta1: $b1, beta2: $b2, gamma1: $g1, gamma2: $g2")

    // Catch all. Matches any message type
    case _ =>
      println("Huh?")
  }

  override def receive = receive1

//  override def preStart(): Unit = {
//    // Perform any initialization setup here
//    // Often this is a good spot to send yourself a message
//    // such as: self ! Initialize
//  }
//
//  override def postStop(): Unit = {
//    // Perform any cleanup here.  The message pump is shut down
//    // so any message you send to yourself will only go to the
//    // dead letter office, but if you'd like to clean up any
//    // resources, such as Database sessions, now's the time to
//    // do it.
//  }

  //Some child actor of MyActor
  //MyActor watches this actor, hence on exception, the strategy of parent(MayActor) would be applied
  class SomeOtherActor extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }
}