package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._

object RequestConsumer {
  def props(settings: Settings) = Props(new RequestConsumer(settings))
}

class RequestConsumer(val settings: Settings) extends Actor with ActorLogging {
  var sessionMap = Map.empty[Long, ActorRef]

  def receive = {
    case request: Request =>
      log.info(s"RequestConsumer received a request $request.")
      if (!sessionMap.contains(request.sessionId)) {
        // create actor
        val tracker = createSessionTracker(
          request.sessionId, settings.sessionTracker.inactivityTimeout
        )
        sessionMap += request.sessionId -> tracker
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
