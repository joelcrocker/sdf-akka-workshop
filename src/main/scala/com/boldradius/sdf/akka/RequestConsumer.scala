package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._

object RequestConsumer {
  def props = Props(new RequestConsumer)
}

class RequestConsumer extends Actor with ActorLogging {
  var sessionMap = Map.empty[Long, ActorRef]
  val inactivityDuration = 20 seconds

  def receive = {
    case request: Request =>
      log.info(s"RequestConsumer received a request $request.")
      if (!sessionMap.contains(request.sessionId)) {
        // create actor
        sessionMap += request.sessionId -> createSessionTracker(request.sessionId, inactivityDuration)
      }
      val tracker = sessionMap(request.sessionId)

      // forward our actor the new request info
      tracker forward request

    case Terminated(tracker) =>
      sessionMap.collect { case (id, `tracker`) => id }.foreach(sessionMap -= _)
  }

  def createSessionTracker(id: Long, inactivityDuration: Duration): ActorRef = {
    val tracker = context.actorOf(SessionTracker.props(
      id, inactivityDuration, context.system.deadLetters), s"session-tracker-${id}")
    context.watch(tracker)
    tracker
  }
}


