package com.boldradius.sdf.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.ClusterEvent._


object DosClusterEventHandler {
  def props(denialOfServicer: ActorRef) = Props(new DosClusterEventHandler(denialOfServicer))

}

class DosClusterEventHandler(denialOfServicer: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info(s"Received member up for ${member.address}")
      if (member.hasRole("consumer")) {
        log.info("Member is a consumer")
        val dosTarget = context.actorSelection(member.address + "/user/consumer")
        denialOfServicer ! DenialOfServicer.Start(dosTarget)
      }
  }
}
