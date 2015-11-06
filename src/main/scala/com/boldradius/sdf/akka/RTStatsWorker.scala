package com.boldradius.sdf.akka

import akka.actor._

import RequestConsumer.{GetSessionMap, SessionMapResponse}
import com.boldradius.sdf.akka.RTStatsWorker.CollectingState
import com.boldradius.sdf.akka.SessionTracker.{GetSessionStats, SessionStats}

object RTStatsWorker {
  import RTStatsQuerier._

  def props(consumer: ActorRef) = Props(new RTStatsWorker(consumer))

  case class CollectingState(
    recipient: ActorRef,
    sessionMap: Map[Long, ActorRef],
    responseBuilder: (Map[String, Long]) => FieldCountResponse
  )
}

class RTStatsWorker(consumer: ActorRef) extends Actor with ActorLogging with Stash {
  import RTStatsQuerier._

  consumer ! GetSessionMap

  var outstandingTrackers = Set[ActorRef]()
  var requestFieldCounts = Map[String, Long]() withDefaultValue 0L

  def receive: Receive = initializing

  def initializing: Receive = {
    case SessionMapResponse(sessionMap) =>
      outstandingTrackers = sessionMap.valuesIterator.toSet
      unstashAll()
      context.become(ready(sessionMap))
    case _ =>
      stash()
  }

  def ready(sessionMap: Map[Long, ActorRef]): Receive = {
    case GetTotalSessionCount =>
      sender ! TotalSessionCount(sessionMap.size)

    case msg @ GetSessionCountPerUrl =>
      val collectingState = CollectingState(sender(), sessionMap, SessionCountPerUrl)
      collectSessions(collectingState)
      context.become(collectingUrlStats(collectingState))

    case msg @ GetSessionCountPerBrowser =>
      val collectingState = CollectingState(sender(), sessionMap, SessionCountPerBrowser)
      collectSessions(collectingState)
      context.become(collectingBrowserStats(collectingState))
  }

  def collectingUrlStats = collectingFieldStats(_.url)_

  def collectingBrowserStats = collectingFieldStats(_.browser)_

  def collectingFieldStats(f: Request => String)(
    state: CollectingState
  ): Receive = {
    case SessionStats(sessionId, requestHistory) =>
      updateFieldCounts(f, requestHistory)
      outstandingTrackers -= sender
      checkCollectedCounts(state)

    case Terminated(tracker) =>
      outstandingTrackers -= sender
      checkCollectedCounts(state)
  }

  def fieldsForSession(f: Request => String, history: Seq[Request]): Seq[String] = {
    history.map(f).distinct
  }

  def updateFieldCounts(f: Request => String, history: Seq[Request]): Unit = {
    val valuesForSession = fieldsForSession(f, history)
    for (value <- valuesForSession) {
      requestFieldCounts += value -> (requestFieldCounts(value) + 1)
    }
  }

  def collectSessions(collectingState: CollectingState): Unit = {
    collectingState.sessionMap.valuesIterator.foreach(_ ! GetSessionStats)
    collectingState.sessionMap.valuesIterator.foreach(context.watch)
    checkCollectedCounts(collectingState)
  }

  def checkCollectedCounts(collectingState: CollectingState): Unit = {
    if (outstandingTrackers.isEmpty) {
      collectingState.recipient ! collectingState.responseBuilder(requestFieldCounts)
      context.stop(self)
    }
  }
}
