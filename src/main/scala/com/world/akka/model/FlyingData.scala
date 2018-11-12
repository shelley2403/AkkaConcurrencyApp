//package com.world.akka.model
//
//import akka.actor.ActorRef
//
//// Helper classes to hold course data
//case class CourseTarget(altitude: Double, heading: Float,
//                        byMillis: Long)
//case class CourseStatus(altitude: Double, heading: Float,
//                        headingSinceMS: Long,
//                        altitudeSinceMS: Long)
//
//
//// The Data that our FlyingBehaviour can hold
//sealed trait Data
//case object Uninitialized extends Data
//// This is the 'real' data.  We're going to stay entirely
//// immutable and, in doing so, we're going to encapsulate
//// all of the changing state
//// data inside this class
//case class FlightData(controls: ActorRef,
//                      elevCalc: Calculator,
//                      bankCalc: Calculator,
//                      target: CourseTarget,
//                      status: CourseStatus) extends Data
//
//class FlyingData FlyingData{
//
//
//  // We're going to allow the FSM to vary the behaviour that calculates the
//  // control changes using this function definition
//  type Calculator = (CourseTarget, CourseStatus) => Any
//
//}
