package com.world.akka.supervisor

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{OneForOneStrategy, SupervisorStrategy, SupervisorStrategyConfigurator}

//If you set akka.actor.guardian-supervisor-strategy to the fully qualified class name of an instance of SupervisorStrategyConfigurator
//then that configurator will be used to create the user guardian's supervisor strategy
//akka {
//    actor {
//     guardian-supervisor-strategy =
//       zzz.akka.UserGuardianStrategyConfigurator
//    }
//  }
class UserGuardianStrategyConfigurator extends SupervisorStrategyConfigurator {
  override def create(): SupervisorStrategy = {
    //OneForOneStrategy(5, 1.minute)
    //as soon as it goes over the threshold the actor will stop and its postStop method will be called (STOP Derivative)
    OneForOneStrategy() {
      case _ => Resume
    }
  }
}

// child of user guardian actor created using system.actorOf uses default supervisor strategy
// can be overridden
