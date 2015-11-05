package com.boldradius.sdf.akka

import akka.actor._
import java.sql.Timestamp


object StatsAggregator {
  // Messages
  case object GetNumberOfRequestsPerBrowser
  case class ResNumberOfRequestsPerBrowser(requestsPerBrowser: Map[String, Long])
  
  case object GetBusiestMinute
  case class ResBusiestMinute(minute: Int, count: Long)
  
  case object GetPageVisitDistribution
  case class ResPageVisitDistribution(percentagePerUrl: Map[String, Double])
  
  case object GetAverageVisitTimePerUrl
  case class ResAverageVisitTimePerUrl(averageVisitPerPage: Map[String, Double])
  
  case object GetTopLandingPages
  case class ResTopLandingPages(urlsWithCount: Seq[(String, Long)])
  
  case object GetTopSinkPages
  case class ResTopSinkPages(urlsWithCount: Seq[(String, Long)])
  
  case object GetTopBrowsers
  case class ResTopBrowsers(userCountByBrowser: Seq[(String, Long)])
  
  case object GetTopReferrers
  case class ResTopReferrers(urlsWithCount: Seq[(String, Long)])
  
  
  def props = Props(new StatsAggregator)

  // Internal state
  case class BrowserStats(requests: Map[String, Long], users: Map[String, Long]) {
    def topBrowsers(count: Int): Seq[(String, Long)] = {
      users.toSeq.sortBy { case (browser, userCount) => userCount }.takeRight(count).reverse
    }
  }

  case class ReferrerStats(referrers: Map[String, Long]) {
    def topReferrers(count: Int): Seq[(String, Long)] = {
      referrers.toSeq.sortBy { case (referrer, count) => count }.takeRight(count).reverse
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

  def statsPerReferrer(oldStats: ReferrerStats, sessionHistory: Seq[Request]): ReferrerStats = {
    val sessionReferrers = sessionHistory.map(_.referrer)

    var newReferrerStats = oldStats.referrers withDefaultValue 0L
    for (referrer <- sessionReferrers) {
      newReferrerStats += referrer -> (newReferrerStats(referrer) + 1)
    }
    ReferrerStats(newReferrerStats)
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
    def topByCount(keep: Int): Seq[(String, Long)] = {
      countByUrl.toSeq.sortBy(-_._2).take(keep).map { case (k, v) => (k, v.toLong) }
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
    val minutes = history.map(request => getMinuteFromTimestamp(request.timestamp))
    for (minute <- minutes) {
      newStatistics += minute -> (newStatistics(minute) + 1)
    }
    newStatistics
  }

  private def getMinuteFromTimestamp(timestamp: Long): Long = {
    // java.util.Date sucks!
    // Using this approximation not accounting for leap seconds.
    timestamp / (1000 * 60) % (24*60)
  }

  def busiestMinute(requestsByMinute: Map[Long, Long]): ResBusiestMinute = {
    val (minute, count) = requestsByMinute.maxBy { case (minute, count) => count }
    ResBusiestMinute(minute.toInt, count)
  }
}

class StatsAggregator extends Actor with ActorLogging {
  import StatsAggregator._

  var requestsPerMinute = Map[Long, Long]()
  var browserStats = BrowserStats(Map.empty, Map.empty)
  var urlVisitDurationStats = Map.empty[String, UrlVisitStats]
  var urlDistributionStats = UrlStats(Map.empty)
  var urlLandingStats = UrlStats(Map.empty)
  var urlSinkStats = UrlStats(Map.empty)
  var referrerStats = ReferrerStats(Map.empty)

  def receive = {
    case SessionTracker.SessionStats(sessionId, history) =>
      browserStats = statsPerBrowser(browserStats, history)
      requestsPerMinute = updatedRequestsPerMinute(requestsPerMinute, history)
      urlDistributionStats = countByPage(urlDistributionStats, history)
      urlVisitDurationStats = statsVisitsPerUrl(urlVisitDurationStats, history)
      urlLandingStats = countPerLanding(urlLandingStats, history)
      urlSinkStats = countPerSink(urlSinkStats, history)
      referrerStats = statsPerReferrer(referrerStats, history)

    case GetNumberOfRequestsPerBrowser =>
      sender() ! ResNumberOfRequestsPerBrowser(browserStats.requests)

    case GetBusiestMinute =>
      sender() ! busiestMinute(requestsPerMinute)

    case GetPageVisitDistribution =>
      sender() ! ResPageVisitDistribution(urlDistributionStats.distribution)

    case GetAverageVisitTimePerUrl =>
      sender() ! ResAverageVisitTimePerUrl(urlVisitDurationStats.mapValues(_.average))

    case GetTopLandingPages =>
      sender() ! ResTopLandingPages(urlLandingStats.topByCount(3))

    case GetTopSinkPages =>
      sender() ! ResTopSinkPages(urlSinkStats.topByCount(3))

    case GetTopBrowsers =>
      sender() ! ResTopBrowsers(browserStats.topBrowsers(2))

    case GetTopReferrers =>
      sender() ! ResTopReferrers(referrerStats.topReferrers(2))
  }
}
