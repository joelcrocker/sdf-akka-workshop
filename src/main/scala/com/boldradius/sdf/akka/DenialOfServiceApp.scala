package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import com.typesafe.config.ConfigFactory


/**
 * Created by lexcellent on 2015-11-06.
 */
object DenialOfServiceApp extends App {
  val config = ConfigFactory.load("denialofservicer")

  val system = ActorSystem("EventCluster", config)

  val settings = new DosSettings(config)

  val denialOfServicer = system.actorOf(DenialOfServicer.props(settings), "denialofservicer")
  val clusterListener = system.actorOf(DosClusterEventHandler.props(denialOfServicer), "dosclustereventhandler")

  Cluster(system).subscribe(clusterListener, classOf[MemberUp])
}
