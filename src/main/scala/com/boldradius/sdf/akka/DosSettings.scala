package com.boldradius.sdf.akka

import com.typesafe.config.{ConfigFactory, Config}
import scala.concurrent.duration._

/**
 * Created by lexcellent on 2015-11-06.
 */
class DosSettings(config: Config) {
  config.checkValid(ConfigFactory.defaultReference(), "denial-of-service")

  protected val dosConfig = config.getConfig("denial-of-service")

  val frequency: FiniteDuration = Duration(dosConfig.getDuration("frequency", MILLISECONDS), MILLISECONDS)
  val sessionIdMax: Long = dosConfig.getLong("session-id-max")
}
