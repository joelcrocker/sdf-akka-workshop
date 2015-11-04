package com.boldradius.sdf.akka

import akka.testkit._

class RequestConsumerSpec extends BaseAkkaSpec {
  "Sending Request to RequestConsumer" should {
    "result in logging" in {
      val consumer = system.actorOf(RequestConsumer.props)
      EventFilter.info(
        source = consumer.path.toString, pattern = ".*received a request.*", occurrences = 1
      ) intercept {
        consumer ! Request(100L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      }
    }
  }
}
