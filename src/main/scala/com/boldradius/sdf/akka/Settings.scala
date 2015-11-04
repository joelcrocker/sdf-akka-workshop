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
}
