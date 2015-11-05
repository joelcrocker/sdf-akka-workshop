package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import com.typesafe.config.ConfigFactory


object RequestConsumerApp extends App {

  val config = ConfigFactory.load("requestconsumer")
  val settings = new ConsumerSettings(config)

  val system = ActorSystem("EventCluster", config)

  val clusterListener = system.actorOf(RequestConsumer.props(settings), "consumer")

  Cluster(system).subscribe(clusterListener, classOf[MemberUp])
}
