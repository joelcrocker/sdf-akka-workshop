package com.boldradius.sdf.akka


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
      import StatsAggegator.UrlStats
      val sessionId = 100L
      def mkRequest(url: String, time: Long) = {
        Request(sessionId, time, url, sim.Session.randomReferrer, sim.Session.randomBrowser)
      }
      val oldStats = Map("url1" -> UrlStats(100, 4))
      // url1 gets 1 visit with 10 ms total, url2 gets 2 visits, with 60 ms total
      val sessionHistory = Seq(
        mkRequest("url1", 1000),
        mkRequest("url2", 1010), // 10 ms
        mkRequest("url2", 1030), // 20 ms
        mkRequest("url1", 1070)  // 40 ms
      )
      val newStats = StatsAggegator.statsPerUrl(oldStats, sessionHistory)
      newStats shouldBe Map("url1" -> UrlStats(110, 5), "url2" -> UrlStats(60, 2))
    }
  }
}
