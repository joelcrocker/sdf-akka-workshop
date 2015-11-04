package com.boldradius.sdf.akka

import akka.actor._

object RequestConsumer {
  def props = Props(new RequestConsumer)
}

class RequestConsumer extends Actor with ActorLogging {
  var sessionMap = Map.empty[Long, ActorRef]

  def receive = {
    case request: Request =>
      log.info(s"RequestConsumer received a request $request.")
      if (!sessionMap.contains(request.sessionId)) {
        // create actor
        val tracker = context.actorOf(SessionTracker.props(request.sessionId))
        sessionMap += request.sessionId -> tracker
      }
      val tracker = sessionMap(request.sessionId)

      // forward our actor the new request info
      tracker forward request
  }
}


