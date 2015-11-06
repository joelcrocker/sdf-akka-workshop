package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._
import RTStatsQuerier._
import scala.concurrent.ExecutionContext.Implicits.global

object RTStatsPoller {
  def props(querier: ActorRef) = Props(new RTStatsPoller(querier))
}

class RTStatsPoller(querier: ActorRef) extends Actor with ActorLogging {

  List(GetTotalSessionCount, GetSessionCountPerBrowser, GetSessionCountPerUrl).foreach(
    context.system.scheduler.schedule(0 seconds, 5 seconds, querier, _)
  )

  def receive: Receive = {
    case TotalSessionCount(count) =>
      log.info(s"Received TotalSessionCount($count)")

    case SessionCountPerBrowser(map) =>
      log.info(s"Received SessionCountPerBrowser($map")

    case SessionCountPerUrl(map) =>
      log.info(s"Received SessionCountPerUrl($map)")
  }
}
