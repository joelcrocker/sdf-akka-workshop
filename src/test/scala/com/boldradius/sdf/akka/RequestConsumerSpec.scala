package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import scala.concurrent.duration._

class RequestConsumerSpec extends BaseAkkaSpec {
  "Sending Request to RequestConsumer" should {
    "result in logging" in {
      val consumer = system.actorOf(RequestConsumer.props(settings))
      EventFilter.info(
        source = consumer.path.toString, pattern = ".*received a request.*", occurrences = 1
      ) intercept {
        consumer ! RequestFactory(100)
      }
    }
    "result in a new SessionTracker" in {
      val consumer = system.actorOf(RequestConsumer.props(settings), "consumer")
      consumer ! RequestFactory(200)
      TestProbe().expectActor("/user/consumer/session-tracker-200")
    }
  }
  "Multiple Requests to RequestConsumer" should {
    "only result in one SessionTracker" in {
      val consumer = TestActorRef(new RequestConsumer(settings))
      consumer ! Request(300L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      consumer ! Request(300L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      consumer.underlyingActor.sessionMap.size shouldEqual 1
    }
  }
}
