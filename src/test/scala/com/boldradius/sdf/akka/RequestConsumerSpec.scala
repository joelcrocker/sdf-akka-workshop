package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit._
import akka.pattern.ask
import akka.util.Timeout
import com.boldradius.sdf.akka.RequestConsumer.{SessionMapResponse, GetSessionMap}
import org.scalatest.concurrent.ScalaFutures
import scala.util.{ Success, Failure }

import scala.concurrent.duration._

class RequestConsumerSpec extends BaseAkkaSpec with ScalaFutures {
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
      implicit val timeout = Timeout(2 seconds)
      val consumer = TestActorRef(new RequestConsumer(settings))
      consumer ! Request(300L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      consumer ! Request(300L, System.currentTimeMillis(), "localhost", "google.com", "chrome")
      val response = (consumer ? GetSessionMap).mapTo[SessionMapResponse]
      whenReady(response) {
        case SessionMapResponse(sessionMap) => sessionMap.size shouldEqual 1
      }
    }
  }
}
