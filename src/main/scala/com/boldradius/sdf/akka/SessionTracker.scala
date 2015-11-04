package com.boldradius.sdf.akka

import akka.actor._

/**
 * Created by Lexcellence on 2015-11-04.
 */
class SessionTracker(sessionId: Long) extends Actor with ActorLogging {

  var history = Vector.empty[Request]
  def receive = {
    case req: Request if sessionId == req.sessionId =>
      log.info("SessionTracker with id {} has received request {}", sessionId, req.toString)
      history :+= req
  }
}

object SessionTracker {
  def props(sessionId: Long) = Props(new SessionTracker(sessionId))
}
