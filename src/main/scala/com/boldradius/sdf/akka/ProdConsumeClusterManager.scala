package com.boldradius.sdf.akka

import akka.actor.{Props, ActorLogging, Actor}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.util.Timeout
import com.boldradius.sdf.akka.sim.RequestProducer
import scala.concurrent.Await
import scala.concurrent.duration._

object ProdConsumeClusterManager {

  def props(settings: ConsumerSettings) = Props(new RequestConsumer(settings))

}

class ProdConsumeClusterManager extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  implicit val resolveTimeout = Timeout(5 seconds)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info(s"Received member up${member.address}")
      if (member.hasRole("producer")) {
        log.info(s"${member.getRoles} up, sending registration to producer")
        val consumerRef = Await.result(context.actorSelection(member.address + "/user/consumer").resolveOne(), resolveTimeout.duration)
        context.actorSelection(member.address + "/user/producer") ! RequestProducer.ConsumerRegistration(consumerRef)
      } else if (member.hasRole("consumer")) {
        log.info(s"Consumer up")
      }

    case MemberRemoved(member, _) =>
      if (member.hasRole("consumer")) {
        // Do stuff
      }
  }
}