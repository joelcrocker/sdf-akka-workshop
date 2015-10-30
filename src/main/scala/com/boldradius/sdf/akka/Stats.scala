package com.boldradius.sdf.akka

import akka.actor.{ActorLogging, Props, Actor}
import akka.persistence.{SaveSnapshotSuccess, SnapshotOffer, PersistentActor}
import com.boldradius.sdf.akka.Stats._

import scala.concurrent.duration._


object Stats{

  def props(persistInterval:FiniteDuration = 10 seconds):Props = Props(new Stats(persistInterval))

  case class SessionStats(sessionId:Long, stats:List[Request])


  case object GetRequestsPerBrowser

  case object Save

  case class StatsAggregate(requestsPerBrowser: Map[String, Int])


  case object SimulatedException extends IllegalStateException("SimulatedException")

  def update(stats:StatsAggregate, requests: List[Request]):StatsAggregate =
  requests.foldLeft(stats)((acc,request) =>
    acc.copy(requestsPerBrowser =
      acc.requestsPerBrowser + (request.browser -> (acc.requestsPerBrowser.getOrElse(request.browser, 0) + 1))))



}

class Stats(persistInterval:FiniteDuration) extends  PersistentActor with ActorLogging{

  override def persistenceId = "stats-id"

  var state: StatsAggregate = StatsAggregate(Map.empty)

  import context.dispatcher

  context.system.scheduler.schedule(persistInterval,persistInterval,self, Save)

  def receiveCommand: Receive = {
    case SessionStats(sessionId, requests) =>
      state = Stats.update(state,requests)


    case GetRequestsPerBrowser => sender() ! state.requestsPerBrowser

    case Save =>
      log.info("Saving Snapshot")
      saveSnapshot(state)


    case SaveSnapshotSuccess(metadata) => log.info("Saved Snapshot")

    case other => log.info("Unhandled msg:" + other)


  }

  def receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: StatsAggregate) => state = snapshot
  }


}
