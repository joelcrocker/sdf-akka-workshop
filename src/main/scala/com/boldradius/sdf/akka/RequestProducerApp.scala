package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberRemoved
import com.boldradius.sdf.akka.sim.RequestProducer
import com.typesafe.config.ConfigFactory

object RequestProducerApp extends App {

  val config = ConfigFactory.load("requestproducer")

  val system = ActorSystem("EventCluster", config)

  val clusterListener = system.actorOf(RequestProducer.props(100), "producer")

  Cluster(system).subscribe(clusterListener, classOf[MemberRemoved])
}
