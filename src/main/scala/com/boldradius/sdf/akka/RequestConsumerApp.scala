package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


object RequestConsumerApp extends App {

  val config = ConfigFactory.load("requestconsumer")
  val settings = new ConsumerSettings(config)

  val system = ActorSystem("EventCluster", config)

  val consumerRef = system.actorOf(RequestConsumer.props(settings), "consumer")

  val managerRef = system.actorOf(ProdConsumeClusterManager.props, "prod-consume-manager")
}
