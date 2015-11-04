package com.boldradius.sdf.akka

import akka.actor._
import java.sql.Timestamp


object StatsAggegator {
  def props = Props(new StatsAggegator)

  case class BrowserStats(requests: Map[String, Long], users: Map[String, Long]) {
    def topTwoBrowsers: Seq[(String, Long)] = {
      users.toSeq.sortBy { case (browser, userCount) => userCount }.takeRight(2).reverse
    }
  }
  case class UrlStats(totalDuration: Long, visitCount: Long) {
    def average: Double = totalDuration.toDouble / visitCount
  }
  type StatsPerUrl = Map[String, UrlStats]

  def statsPerBrowser(oldStats: BrowserStats, sessionHistory: Seq[Request]) : BrowserStats = {
    val sessionRequestStats = sessionHistory.groupBy(_.browser).map {
      case (browser, requests) => browser -> requests.size
    }

    var newRequestStats = oldStats.requests withDefaultValue 0L
    var newUserStats = oldStats.users withDefaultValue 0L
    for ((browser, count) <- sessionRequestStats) {
      newRequestStats += browser -> (newRequestStats(browser) + count)
      newUserStats += browser -> (newUserStats(browser) + 1)
    }
    BrowserStats(newRequestStats, newUserStats)
  }

  def statsPerUrl(oldStats: StatsPerUrl, sessionHistory: Seq[Request]): StatsPerUrl = {
    var newStats = oldStats withDefaultValue UrlStats(0, 0)
    for (Seq(prev, next) <- sessionHistory.sliding(2)) {
      val duration = next.timestamp - prev.timestamp
      val url = prev.url
      val urlStats = newStats(url)
      newStats += url -> urlStats.copy(urlStats.totalDuration + duration, urlStats.visitCount + 1)
    }
    newStats
  }

  def updatedRequestsPerMinute(oldStatistics: Map[Long, Long], history: Seq[Request]): Map[Long, Long] = {
    var newStatistics = oldStatistics withDefaultValue 0L
    val minutes = history.map(_.timestamp).map(getMinuteFromTimestamp(_))
    for (minute <- minutes) {
      newStatistics += minute -> (newStatistics(minute) + 1)
    }
    newStatistics
  }

  def getMinuteFromTimestamp(timestamp: Long): Long = {
    val date: Timestamp = new Timestamp(timestamp)
    val minuteOfDay = date.getHours() * 60 + date.getMinutes()
    minuteOfDay
  }

  def busiestMinute(statistics: Map[Long, Long]): Long = {
    val (minute, _) = statistics.reduceLeft[(Long, Long)]((kv1, kv2) => if (kv1._2 > kv2._2) kv1 else kv2)
    minute
  }
}

class StatsAggegator extends Actor with ActorLogging {
  import StatsAggegator._

  var requestsPerMinute = Map[Long, Long]()

  def receive = {
    case SessionTracker.SessionStats(sessionId, history) =>
      requestsPerMinute = updatedRequestsPerMinute(requestsPerMinute, history)
  }
}
