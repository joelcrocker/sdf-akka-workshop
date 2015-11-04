package com.boldradius.sdf.akka

import akka.actor._
import java.sql.Timestamp


object StatsAggegator {
  // Messages
  case object GetNumberOfRequestsPerBrowser
  case class ResNumerOfRequetsPerBrowser(requestsPerBrowser: Map[String, Long])
  
  case object GetBusiestMinute
  case class ResBusiestMinute(minute: Int, count: Long)
  
  case object GetPageVisitDistribution
  case class ResPageVisitDistribution()
  
  case object GetAverageVisitTimePerUrl
  case class ResAverageVisitTimePerUrl(averageVisitPerPage: Map[String, Double])
  
  case object GetTopLandingPages
  case class ResTopLandingPages()
  
  case object GetTopSinkPages
  case class ResTopSinkPages()
  
  case object GetTopBrowsers
  case class ResTopBrowsers(userCountByBrowser: Seq[(String, Long)])
  
  case object GetTopReferrals
  case class ResTopReferrals()
  
  
  def props = Props(new StatsAggegator)

  // Internal state
  case class BrowserStats(requests: Map[String, Long], users: Map[String, Long]) {
    def topBrowsers(count: Int): Seq[(String, Long)] = {
      users.toSeq.sortBy { case (browser, userCount) => userCount }.takeRight(count).reverse
    }
  }
  case class UrlVisitStats(totalDuration: Long, visitCount: Long) {
    def average: Double = totalDuration.toDouble / visitCount
  }
  type StatsPerUrl = Map[String, UrlVisitStats]

  def statsPerBrowser(oldStats: BrowserStats, sessionHistory: Seq[Request]): BrowserStats = {
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

  case class UrlStats(val countByUrl: Map[String, Int]) {
    def increment(url: String, count: Int): UrlStats = {
      countByUrl.get(url) match {
        case None => UrlStats(countByUrl + (url -> count))
        case Some(oldCount: Int) => UrlStats(countByUrl + (url -> (oldCount + count)))
      }
    }
    def urlPercentage(url: String): Int = {
      countByUrl.get(url) match {
        case None => 0
        case Some(count) => ((100.0 * count)/countByUrl.values.sum).toInt
      }
    }
    def urlCount(url: String): Int = {
      countByUrl.get(url) match {
        case None => 0
        case Some(count) => count
      }
    }
    def distribution: Map[String, Double] = {
      val totalCount = countByUrl.values.sum
      countByUrl.mapValues(_.toDouble / totalCount)
    }
    def topCount: (String, Int) = {
      countByUrl.maxBy(_._2)
    }
  }

  def countPerSink(oldStatistics: UrlStats, history: Seq[Request])
  : UrlStats = {
    history.lastOption match { // the "Sink" page
      case Some(req: Request) => oldStatistics.increment(req.url, 1)
      case None => oldStatistics
    }
  }

  def countPerLanding(oldStatistics: UrlStats, history: Seq[Request])
  : UrlStats = {
    history.headOption match { // the "Landing" page
      case Some(req: Request) => oldStatistics.increment(req.url, 1)
      case None => oldStatistics
    }
  }

  def countByPage(oldStatistics: UrlStats, history: Seq[Request])
  : UrlStats = {
    var newStatistics = oldStatistics.countByUrl withDefaultValue 0
    val current = history.groupBy(_.url).map {
      case (url, requests) => url -> requests.size
    }
    for ((url, count) <- current) {
      newStatistics += url -> (newStatistics(url) + count)
    }
    UrlStats(newStatistics)
  }

  def statsVisitsPerUrl(oldStats: StatsPerUrl, sessionHistory: Seq[Request]): StatsPerUrl = {
    var newStats = oldStats withDefaultValue UrlVisitStats(0, 0)
    for (Seq(prev, next) <- sessionHistory.sliding(2)) {
      val duration = next.timestamp - prev.timestamp
      val url = prev.url
      val urlStats = newStats(url)
      newStats += url -> urlStats.copy(urlStats.totalDuration + duration, urlStats.visitCount + 1)
    }
    newStats
  }

  def updatedRequestsPerMinute(oldStatistics: Map[Long, Long], history: Seq[Request])
  : Map[Long, Long] = {
    var newStatistics = oldStatistics withDefaultValue 0L
    val minutes = history.map(_.timestamp).map(getMinuteFromTimestamp(_))
    for (minute <- minutes) {
      newStatistics += minute -> (newStatistics(minute) + 1)
    }
    newStatistics
  }

  private def getMinuteFromTimestamp(timestamp: Long): Long = {
    val date: Timestamp = new Timestamp(timestamp)
    val minuteOfDay = date.getHours() * 60 + date.getMinutes()
    minuteOfDay
  }

  def busiestMinute(statistics: Map[Long, Long]): ResBusiestMinute = {
    val (minute, count) =
      statistics.reduceLeft[(Long, Long)]((kv1, kv2) => if (kv1._2 > kv2._2) kv1 else kv2)
    ResBusiestMinute(minute.toInt, count)
  }
}

class StatsAggegator extends Actor with ActorLogging {
  import StatsAggegator._

  var requestsPerMinute = Map[Long, Long]()
  var browserStats = BrowserStats(Map.empty, Map.empty)
  var urlVisitDurationStats = Map.empty[String, UrlVisitStats]
  var urlDistributionStats = UrlStats(Map.empty)
  var urlLandingStats = UrlStats(Map.empty)
  var urlSinkStats = UrlStats(Map.empty)

  def receive = {
    case SessionTracker.SessionStats(sessionId, history) =>
      browserStats = statsPerBrowser(browserStats, history)
      requestsPerMinute = updatedRequestsPerMinute(requestsPerMinute, history)
      urlDistributionStats = countByPage(urlDistributionStats, history)
      urlVisitDurationStats = statsVisitsPerUrl(urlVisitDurationStats, history)
      urlLandingStats = countPerLanding(urlLandingStats, history)
      urlSinkStats = countPerSink(urlSinkStats, history)

    case GetNumberOfRequestsPerBrowser =>
      sender() ! ResNumerOfRequetsPerBrowser(browserStats.requests)

    case GetBusiestMinute =>
      sender() ! busiestMinute(requestsPerMinute)

    case GetPageVisitDistribution =>
      sender() ! ResPageVisitDistribution

    case GetAverageVisitTimePerUrl =>
      sender() ! ResAverageVisitTimePerUrl(urlVisitDurationStats.mapValues(_.average))

    case GetTopLandingPages =>
      sender() ! ResTopLandingPages()

    case GetTopSinkPages =>
      sender() ! ResTopSinkPages()

    case GetTopBrowsers =>
      sender() ! ResTopBrowsers(browserStats.topBrowsers(2))

    case GetTopReferrals =>
      sender() ! ResTopReferrals()
  }
}
