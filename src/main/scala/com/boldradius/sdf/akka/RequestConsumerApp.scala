package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterDomainEvent
import com.typesafe.config.ConfigFactory


object RequestConsumerApp extends App {

  val config = ConfigFactory.load("requestconsumer")

  System.setProperty("akka.remote.netty.tcp.port", "2552")

  val system = ActorSystem("EventCluster", config)

  val clusterListener = system.actorOf(RequestConsumer.props(), "consumer")

  Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
}
