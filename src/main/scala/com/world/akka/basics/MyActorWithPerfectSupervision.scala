package com.world.akka.basics

import akka.actor.{Actor, Terminated}


//Here we use the default strategy while overriding the preRestart and posrRestartMethod
//Scenario 1 -> The child of MyActorWithPerfectSupervision dies (SomeOtherChildActor); it will trigger terminate message and will recreate the failed children
//Scenario 2-> When the MyActorWithPerfectSupervision restarts due to exception, calls the preRestart (postStop()) and doesn't stop the children. This will not trigger a Terminate message and not result in recreating the children
//              Calls the postRestart method (preStart) method and doesn't recreate the children.
class MyActorWithPerfectSupervision extends Actor{

    def initialize() {
      // Do your initialization here
    }

    override def preStart() {
      initialize()
      // Start your children here
    }

    override def preRestart(reason: Throwable,
                            message: Option[Any]) {
      // The default behaviour was to stop the children
      // here but we don't want to do that

      // We still want to postStop() however.
      postStop()
    }

    override def postRestart(reason: Throwable) {
      // The default behaviour was to call preStart()
      // but we don't want to do that, since that's
      // where children get started
      initialize()
    }

    def receive = {
      case Terminated(child) =>
      // re-create the failed child.  Now it's OK,
      // since the only reason we can get this message
      // is because the child really died without
      // our help
    }
}

class SomeOtherChildActor extends Actor {
  override def receive: Receive = ???
}
