package com.boldradius.sdf.akka

import akka.actor.{Props, ActorLogging, Actor}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.boldradius.sdf.akka.sim.RequestProducer

object ClusterManager {

  def props(settings: ConsumerSettings) = Props(new RequestConsumer(settings))

}

class ClusterManager extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info(s"Received member up${member.address}")
      if (member.hasRole("producer") || member.hasRole("consumer")) {
        log.info(s"${member.getRoles} up, sending registration to producer")
        context.actorSelection(member.address + "/user/producer") ! RequestProducer.ConsumerRegistration(context.actorFor(member.address + "/user/consumer"))
      } else if (member.hasRole("consumer")) {
        log.info(s"Consumer up")
      }

    case MemberRemoved(member, _) =>
      if (member.hasRole("consumer")) {
        context.actorSelection(member.address + "/user/producer") ! RequestProducer.ConsumerRegistration(context.actorFor(member.address + "/user/consumer"))
      }
  }
}