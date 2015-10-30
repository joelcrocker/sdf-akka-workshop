package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.SimulationActor.StartApp
import scala.io.StdIn
import scala.concurrent.duration._

object  RequestSimulationExampleApp extends App {


  // First, we create an actor system, a producer and a consumer
  val system = ActorSystem("EventProducerExample")


  val simulationApp = system.actorOf(Props(new SimulationActor), "sim-actor")

  // Tell the producer to start working and to send messages to the consumer
  simulationApp ! StartApp



//  // Wait for the user to hit <enter>
//  println("Hit <enter> to stop the simulation")
//  StdIn.readLine()
//
//
//  system.stop(simulationApp)
//
//  // Terminate all actors and wait for graceful shutdown
//  system.shutdown()
//  system.awaitTermination(10 seconds)
}


