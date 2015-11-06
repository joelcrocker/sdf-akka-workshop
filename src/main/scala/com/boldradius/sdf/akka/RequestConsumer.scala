package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestConsumer.{GetSessionMap, SessionMapResponse}
import scala.concurrent.duration._

object RequestConsumer {
  def props(settings: ConsumerSettings) = Props(new RequestConsumer(settings))

  case object GetSessionMap
  case class SessionMapResponse(sessionMap: Map[Long, ActorRef])
}

class RequestConsumer(val settings: ConsumerSettings) extends Actor with ActorLogging with Stash {
  var sessionMap = Map.empty[Long, ActorRef]
  val alerter = context.actorOf(Alerter.props)
  val statsSupervisor = createStatsSupervisor()

  override def receive: Receive = {
    case StatsSupervisor.StatsAggregatorResponse(aggregator) =>
      unstashAll()
      context.become(ready(aggregator))
    case _ =>
      stash()
  }

  def ready(statsAggregator: ActorRef): Receive = {
    case request: Request =>
      log.info(s"RequestConsumer received a request $request.")
      if (!sessionMap.contains(request.sessionId)) {
        // create actor
        val tracker = createSessionTracker(
          request.sessionId, settings.sessionTracker.inactivityTimeout, statsAggregator
        )
        sessionMap += request.sessionId -> tracker
      }
      val tracker = sessionMap(request.sessionId)

      // forward our actor the new request info
      tracker forward request

    case GetSessionMap => sender ! SessionMapResponse(sessionMap)

    case Terminated(tracker) =>
      sessionMap.collect { case (id, `tracker`) => id }.foreach(sessionMap -= _)

    case _ =>
      log.info("Received unhandled message in RequestConsumer")
  }

  def createSessionTracker(id: Long, inactivityDuration: Duration, statsAggregator: ActorRef)
  : ActorRef = {
    val tracker = context.actorOf(SessionTracker.props(
      id, inactivityDuration, statsAggregator), s"session-tracker-${id}")
    context.watch(tracker)
    tracker
  }

  def createStatsSupervisor(): ActorRef = {
    val supervisor = context.actorOf(StatsSupervisor.props(alerter, settings))
    supervisor ! StatsSupervisor.GetStatsAggregator
    supervisor
  }
}
