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
