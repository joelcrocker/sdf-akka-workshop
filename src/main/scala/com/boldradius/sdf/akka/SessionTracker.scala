package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._


class SessionTracker(sessionId: Long, inactivityTimeout: Duration, statsCollector: ActorRef) extends Actor with ActorLogging {
  import SessionTracker._

  var history = Vector.empty[Request]

  context.setReceiveTimeout(inactivityTimeout)

  def receive = {
    case req: Request if sessionId == req.sessionId =>
      log.debug("SessionTracker with id {} has received request {}", sessionId, req.toString)
      history :+= req

    case ReceiveTimeout =>
      log.info(s"Session inactive: $sessionId")
      statsCollector ! SessionStats(sessionId, history)
      context.stop(self)
  }
}

object SessionTracker {
  def props(sessionId: Long, inactivityTimeout: Duration, statsCollector: ActorRef) =
    Props(new SessionTracker(sessionId, inactivityTimeout, statsCollector))

  case class SessionStats(sessionId: Long, requestHistory: Seq[Request])
}
