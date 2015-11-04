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

  def countPerSink(oldStatistics: Map[String, Long], history: Seq[Request])
  : Map[String, Long] = {
    history.lastOption.match { // the "Sink" page
      case Some(req: Request) =>  oldStatistics + (req.url -> 1L)
      case None => oldStatistics
    }
  }

  def countPerLanding(oldStatistics: Map[String, Long], history: Seq[Request])
  : Map[String, Long] = {
    history.headOption.match { // the "Landing" page
      case Some(req: Request) =>  oldStatistics + (req.url -> 1L)
      case None => oldStatistics
    }
  }

  def countByPage(oldStatistics: Map[String, Long], history: Seq[Request])
  : Map[String, Long] = {
    var newStatistics = oldStatistics withDefaultValue 0L
    val current = history.groupBy(_.url).map {
      case (url, requests) => url -> requests.size
    }
    for ((url, count) <- current) {
      newStatistics += url -> (newStatistics(url) + count)
    }
    newStatistics
  }
}

class StatsAggegator extends Actor with ActorLogging {
  def receive = {
    case SessionTracker.SessionStats(sessionId, history) =>

  }
}
