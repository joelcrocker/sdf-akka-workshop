package com.boldradius.sdf.akka

import akka.testkit.{TestActorRef, EventFilter, TestProbe}
import akka.util.Timeout
import com.boldradius.sdf.akka.Consumer._
import com.boldradius.sdf.akka.RealTimeSessionStats.RunStatsResponse
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import akka.pattern.ask

class ConsumerSpec extends BaseAkkaSpec with ScalaFutures{


  "Sending message to Consumer" should {
    "result in logging " in {
      EventFilter.info(pattern = ".*Received Request.*", occurrences = 1) intercept {
        val consumer = system.actorOf(Consumer.props(2 seconds), "consumerActor")
        consumer ! Request(0, System.currentTimeMillis(), "url", "referrer", "browser")
      }
    }
    "result in creation of SessionStateLog " in {
      val consumer = system.actorOf(Consumer.props(2 seconds), "consumer-test-2")
      consumer ! Request(1, System.currentTimeMillis(), "url", "referrer", "browser")
      TestProbe().expectActor("/user/consumer-test-2/$*")

    }
  }

  "Receiving a SessionInactive msg" should {
    "result in sending a SessionStats msg to parent" in {
      val inactivetimeout = 1 seconds
      val waitFor = 2 seconds

      val parent = TestProbe()
      val underTest = TestActorRef[Consumer]( Consumer.props(inactivetimeout), parent.ref, "child-consumer")

      underTest ! Request(1, 1, "url", "referrer", "browser")
//      underTest ! SessionInactive(0,List( Request(0, 1, "url", "referrer", "browser") ))
      parent.within(waitFor) {
        parent.expectMsg(Stats.SessionStats(1, List( Request(1, 1, "url", "referrer", "browser"))))
      }
    }
  }

  "Receiving a RunStats msg" should {
    "result in RunStatsResponse" in {
      val parent = TestProbe()
      val underTest = TestActorRef[Consumer]( Consumer.props(20 seconds), parent.ref, "child-consumer")
      underTest ! Request(1, 1, "url1", "referrer", "browser1")
      underTest ! Request(2, 1, "url2", "referrer", "browser1")
      underTest ! Request(3, 1, "url3", "referrer", "browser2")
      underTest ! Request(4, 1, "url4", "referrer", "browser3")
      underTest ! Request(5, 1, "url4", "referrer", "browser3")
      underTest ! Request(6, 1, "url4", "referrer", "browser3")


      implicit val timeout:Timeout = Timeout(2 seconds)
      whenReady( (underTest ? GetSessionTotal).mapTo[SessionTotal]){ total =>
        assert(total == SessionTotal(6))
      }


      whenReady( (underTest ? GetSessionsPerBrowser).mapTo[RunStatsResponse]){ total =>
        assert(total == RunStatsResponse(Map("browser1" -> 2, "browser3" -> 3, "browser2" -> 1)))
      }

      whenReady( (underTest ? GetSessionsPerUrl).mapTo[RunStatsResponse]){ total =>
        assert(total == RunStatsResponse(Map("url1" -> 1, "url2" -> 1, "url3" -> 1, "url4" -> 3)))
      }



    }
  }



}
