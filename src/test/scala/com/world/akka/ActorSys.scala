package com.world.akka

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.fixture

object ActorSys {
  val uniqueId = new AtomicInteger(0)
}

// this is a helper class that will handle the ActorSystem to get true isolation between tests
class ActorSys(name: String) extends TestKit(ActorSystem(name)) with ImplicitSender with fixture.NoArg {
  // Hides the ActorSystem and gives no arg constructor
  def this() = this(
    "TestSystem%05d".format(
      ActorSys.uniqueId.getAndIncrement()))

  def shutdown(): Unit = system.terminate()

  override def apply() {
    try super.apply()
    finally shutdown()
  }
}
