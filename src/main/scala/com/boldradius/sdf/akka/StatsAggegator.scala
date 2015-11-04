package com.boldradius.sdf.akka

import akka.actor._


object StatsAggegator {
  def props = Props(new StatsAggegator)

  def numberOfRequestsPerBrowser(oldStatistics: Map[String, Long], history: Seq[Request])
  : Map[String, Long] = {
    var newStatistics = oldStatistics withDefaultValue 0L
    val current = history.groupBy(_.browser).map {
      case (browser, requests) => browser -> requests.size
    }
    for ((browser, count) <- current) {
      newStatistics += browser -> (newStatistics(browser) + count)
    }
    newStatistics
  }
}

class StatsAggegator extends Actor with ActorLogging {
  def receive = {
    case SessionTracker.SessionStats(sessionId, history) =>

  }
}
