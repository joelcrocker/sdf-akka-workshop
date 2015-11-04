package com.boldradius.sdf.akka

import akka.actor._


object StatsAggegator {
  def props = Props(new StatsAggegator)

  case class BrowserStats(requests: Map[String, Long], users: Map[String, Long])

  def statsPerBrowser(oldStats: BrowserStats, sessionHistory: Seq[Request])
  : BrowserStats = {
    val sessionRequestStats = sessionHistory.groupBy(_.browser).map {
      case (browser, requests) => browser -> requests.size
    }

    var newRequestStats = oldStats.requests withDefaultValue 0L
    var newUserStats = oldStats.users withDefaultValue 0L
    for ((browser, count) <- sessionRequestStats) {
      newRequestStats += browser -> (newRequestStats(browser) + count)
      newUserStats += browser -> (newRequestStats(browser) + 1)
    }
    BrowserStats(newRequestStats, newUserStats)
  }
}

class StatsAggegator extends Actor with ActorLogging {
  def receive = {
    case SessionTracker.SessionStats(sessionId, history) =>

  }
}
