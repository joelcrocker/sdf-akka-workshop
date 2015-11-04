package com.boldradius.sdf.akka

import scala.concurrent.duration._

class StatsAggregatorSpec extends BaseAkkaSpec {

  "StatsAggregator" should {
    "compute stats per browser" in {
      val sessionId = 100L
      def mkRequest(browser: String) = {
        Request(sessionId, System.currentTimeMillis(), sim.Session.randomUrl,
          sim.Session.randomReferrer, browser
        )
      }
      val oldStats = StatsAggegator.BrowserStats(
        requests = Map("chrome" -> 100),
        users = Map("chrome" -> 5)
      )
      val sessionHistory = {
        (0 until 5).map(_ => mkRequest("chrome")) ++
          (0 until 7).map(_ => mkRequest("firefox"))
      }
      val newStats = StatsAggegator.statsPerBrowser(oldStats, sessionHistory)
      newStats.requests shouldBe Map("chrome" -> 105, "firefox" -> 7)
      newStats.users shouldBe  Map("chrome" -> 6, "firefox" -> 1)
    }

    "compute stats per url" in {
      import StatsAggegator.UrlVisitStats
      val sessionId = 100L
      def mkRequest(url: String, time: Long) = {
        Request(sessionId, time, url, sim.Session.randomReferrer, sim.Session.randomBrowser)
      }
      val oldStats = Map("url1" -> UrlVisitStats(100, 4))
      // url1 gets 1 visit with 10 ms total, url2 gets 2 visits, with 60 ms total
      val sessionHistory = Seq(
        mkRequest("url1", 1000),
        mkRequest("url2", 1010), // 10 ms
        mkRequest("url2", 1030), // 20 ms
        mkRequest("url1", 1070)  // 40 ms
      )
      val newStats = StatsAggegator.statsPerUrl(oldStats, sessionHistory)
      newStats shouldBe Map("url1" -> UrlVisitStats(110, 5), "url2" -> UrlVisitStats(60, 2))
    }
  }

  "Busiest request per minute" should {
    "return the expected value" in {
      val oldStats = Map(50L -> 100L, 60L -> 200L)

      val requestHistory = Seq(
        RequestFactory(sessionId=500L, timestamp = (1 minute).toMillis),
        RequestFactory(sessionId=600L, timestamp = (5 minute).toMillis),
        RequestFactory(sessionId=600L, timestamp = (5 minute).toMillis),
        RequestFactory(sessionId=600L, timestamp = (5 minute).toMillis),
        RequestFactory(sessionId=600L, timestamp = (5 minute).toMillis)
      )

      val newStats = StatsAggegator.updatedRequestsPerMinute(oldStats, requestHistory)

      val busiestMinute = StatsAggegator.busiestMinute(newStats)
      busiestMinute shouldEqual 60L
    }

    "compute stats per url" should {
      val oldStats = StatsAggegator.UrlStats(
        countByUrl = Map("yahoo.com" -> 10, "boldradius.com" -> 5, "leveloflexcellence.com" -> 1)
      )
      val sessionHistory = Seq(
        RequestFactory(100L, url = "yahoo.com"),
        RequestFactory(100L, url = "gmail.com"),
        RequestFactory(100L, url = "hotmail.com"),
        RequestFactory(100L, url = "pagerduty.com")
      )
      val newStats = StatsAggegator.countByPage(oldStats, sessionHistory)

      "overall url counts" in {
        newStats.urlCount("yahoo.com") shouldEqual 11
        newStats.urlCount("pagerduty.com") shouldEqual 1
      }

      "urls by percentage" in {
        newStats.urlPercentage("yahoo.com") shouldEqual 55
        newStats.urlPercentage("pagerduty.com") shouldEqual 5
      }
    }

    "compute count per sink correctly" in {
      val oldSinkStats = StatsAggegator.UrlStats(
        countByUrl = Map("gmail.com/logout" -> 10, "boldradius.com/logout" -> 5)
      )
      val sessionHistory = Seq(
        RequestFactory(100L, url = "boldradius.com/logout"),
        RequestFactory(100L, url = "hotmail.com"),
        RequestFactory(100L, url = "gmail.com/logout")
      )
      val newSinkStats = StatsAggegator.countPerSink(oldSinkStats, sessionHistory)
      newSinkStats.urlCount("gmail.com/logout") shouldEqual 11
      newSinkStats.urlCount("boldradius.com/logout") shouldEqual 5
    }

    "compute count per landing correctly" in {
      val oldLandingStats = StatsAggegator.UrlStats(
        countByUrl = Map("gmail.com/logout" -> 10, "boldradius.com/logout" -> 5)
      )
      val sessionHistory = Seq(
        RequestFactory(100L, url = "boldradius.com/logout"),
        RequestFactory(100L, url = "hotmail.com"),
        RequestFactory(100L, url = "gmail.com/logout")
      )
      val newLandingStats = StatsAggegator.countPerLanding(oldLandingStats, sessionHistory)
      newLandingStats.urlCount("gmail.com/logout") shouldEqual 10
      newLandingStats.urlCount("boldradius.com/logout") shouldEqual 6
    }
  }
}
