package com.boldradius.sdf.akka

import akka.actor._

object RTStatsQuerier {

  def props(consumer: ActorRef) = Props(new RTStatsQuerier(consumer))

  sealed trait RTStatsRequest
  sealed class FieldCountResponse(fieldSessions: Map[String, Long])

  case object GetTotalSessionCount extends RTStatsRequest
  case class TotalSessionCount(count: Int)

  case object GetSessionCountPerUrl extends RTStatsRequest
  case class SessionCountPerUrl(fieldSessions: Map[String, Long]) extends FieldCountResponse(fieldSessions)

  case object GetSessionCountPerBrowser extends RTStatsRequest
  case class SessionCountPerBrowser(fieldSessions: Map[String, Long]) extends FieldCountResponse(fieldSessions)
}

class RTStatsQuerier(consumer: ActorRef) extends Actor with ActorLogging {
  import RTStatsQuerier._

  def receive: Receive = {
    case msg: RTStatsRequest =>
      createWorker() forward msg
  }

  def createWorker(): ActorRef =
    context.actorOf(RTStatsWorker.props(consumer))
}
