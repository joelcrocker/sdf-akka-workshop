package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestConsumer.{SessionMapResponse, GetSessionMap}
import scala.concurrent.duration._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object RequestConsumer {
  def props(settings: Settings) = Props(new RequestConsumer(settings))

  case object GetSessionMap
  case class SessionMapResponse(sessionMap: Map[Long, ActorRef])
}

class RequestConsumer(val settings: Settings) extends Actor with ActorLogging with Stash {
  var sessionMap = Map.empty[Long, ActorRef]
  val alerter = context.actorOf(Alerter.props)
  val statsSupervisor = createStatsSupervisor()

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

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

    case MemberUp(member) =>
      if (member.hasRole("producer"))
        sender() ! context.system.deadLetters

    case Terminated(tracker) =>
      sessionMap.collect { case (id, `tracker`) => id }.foreach(sessionMap -= _)
  }

  def createSessionTracker(id: Long, inactivityDuration: Duration, statsAggregator: ActorRef)
  : ActorRef = {
    val tracker = context.actorOf(SessionTracker.props(
      id, inactivityDuration, statsAggregator), s"session-tracker-${id}")
    context.watch(tracker)
    tracker
  }

  def createStatsSupervisor(): ActorRef = {
    val supervisor = context.actorOf(StatsSupervisor.props(
      alerter,
      settings.statsSupervisor.maxRetries,
      settings.statsSupervisor.retryTimeRange
    ))
    supervisor ! StatsSupervisor.GetStatsAggregator
    supervisor
  }
}
