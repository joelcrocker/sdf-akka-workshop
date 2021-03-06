package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.sim._


object  RequestSimulationExampleApp extends App {

  // First, we create an actor system, a producer and a consumer
  val system = ActorSystem("EventProducerExample")

  val simulationApp = new RequestSimulationExampleApp(system)
}


class RequestSimulationExampleApp(system:ActorSystem){

  val settings = new ConsumerSettings()

  val producer = system.actorOf(RequestProducer.props(100), "producer")
  val consumer = system.actorOf(RequestConsumer.props(settings), "consumer")
  val querier = system.actorOf(RTStatsQuerier.props(consumer), "rtstats-querier")
  val rtStatsPoller = system.actorOf(RTStatsPoller.props(querier), "rtstats-poller")

  // Tell the producer to start working and to send messages to the consumer
  producer ! RequestProducer.ConsumerRegistration(consumer)

  def stop() = {
    // Tell the producer to stop working
    producer ! RequestProducer.Stop
  }
}
