package com.boldradius.sdf.akka

import org.scalatest.BeforeAndAfter


class StatsAggregatorSpec extends BaseAkkaSpec {

  "StatsAggregator" should {
    "work correctly when computing stats per browser" in {

      val oldStats = StatsAggegator.BrowserStats(
        requests = Map("chrome" -> 100),
        users = Map("chrome" -> 5)
      )
      val sessionHistory = ???
      val newStats = StatsAggegator.statsPerBrowser(oldStats, sessionHistory)
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
