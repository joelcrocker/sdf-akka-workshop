package com.boldradius.sdf.akka

import com.typesafe.config.{ConfigFactory, Config}
import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration._


class Settings(config: Config = ConfigFactory.load()) {

  config.checkValid(ConfigFactory.defaultReference(), "web-stats")
  protected val appConfig = config.getConfig("web-stats")

  object sessionTracker {
    val inactivityTimeout: FiniteDuration = {
      appConfig.getDuration("session-tracker.inactivity-timeout", MILLISECONDS).millis
    }
  }

  object statsSupervisor {
    val maxRetries: Int = { appConfig.getInt("stats-supervisor.max-retries") }
    val retryTimeRange: Duration = {
      appConfig.getDuration("stats-supervisor.retry-time-range", MILLISECONDS).millis
    }
  }

  // Init objects
  sessionTracker
  statsSupervisor
}
