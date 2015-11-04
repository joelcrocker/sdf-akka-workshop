package com.boldradius.sdf.akka


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
  }
}
