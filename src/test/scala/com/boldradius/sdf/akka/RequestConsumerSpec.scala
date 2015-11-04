package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import scala.concurrent.duration._

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
    "result in a new SessionTracker" in {
      val consumer = system.actorOf(RequestConsumer.props, "consumer")
      consumer ! Request(200L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      TestProbe().expectActor("/user/consumer/session-tracker-200")
    }
  }
  "Multiple Requests to RequestConsumer" should {
    "only result in one SessionTracker" in {
      val consumer = TestActorRef(new RequestConsumer)
      consumer ! Request(300L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      consumer ! Request(300L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      consumer.underlyingActor.sessionMap.size shouldEqual 1
    }
  }
}
