package com.boldradius.sdf.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.ClusterEvent._
import akka.util.Timeout
import scala.concurrent.duration._


object DosClusterEventHandler {
  def props(denialOfServicer: ActorRef) = Props(new DosClusterEventHandler(denialOfServicer))

}

class DosClusterEventHandler(denialOfServicer: ActorRef)(implicit timeout: Timeout = 10 seconds)
    extends Actor with ActorLogging {

  import context.dispatcher

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info(s"Received member up for ${member.address}")
      if (member.hasRole("consumer")) {
        log.info("Member is a consumer")
        context.actorSelection(member.address + "/user/consumer").resolveOne() map { x =>
          denialOfServicer ! DenialOfServicer.Start(x)
        }
      }
  }
}
