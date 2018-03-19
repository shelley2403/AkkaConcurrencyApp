package com.world.akka

import akka.actor.{ActorSystem, Props}

object BadShakespeareanMain extends App{
  val system = ActorSystem("BadShakespearean")
  // 'Props' gives us a way to modify certain aspects of an
  // Actor's structure

//  'val actor = context.actorOf(Props[MyActor])'
//  (to create a supervised child actor from within an actor), or
//  'val actor = system.actorOf(Props(new MyActor(..)))'
//  (to create a top level actor from the ActorSystem)
  val actor = system.actorOf(Props[BadShakespeareanActor], "BadShake")
  actor ! "Good Morning"
  actor ! "Hi there"
  system.terminate()

}
