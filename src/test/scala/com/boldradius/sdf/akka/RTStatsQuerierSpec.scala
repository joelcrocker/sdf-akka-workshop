package com.boldradius.sdf.akka

import akka.testkit._
import com.boldradius.sdf.akka.RTStatsQuerier._

class RTStatsQuerierSpec extends BaseAkkaSpec {
  "Querying for total session count" should {
    "return the correct result with no trackers" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetTotalSessionCount, client.ref)
      client.expectMsg(TotalSessionCount(0))
    }

    "return the correct result with multiple trackers" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      (1 to 6).foreach(consumer ! RequestFactory(_))
      (1 to 4).foreach(consumer ! RequestFactory(_))

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetTotalSessionCount, client.ref)
      client.expectMsg(TotalSessionCount(6))
    }
  }

  "Querying for sessions per url" should {
    "return the correct result with no trackers" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetSessionCountPerUrl, client.ref)
      client.expectMsg(SessionCountPerUrl(Map()))
    }

    "return the correct result with multiple trackers" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      (1 to 6).foreach(consumer ! RequestFactory(_))
      (1 to 4).foreach(consumer ! RequestFactory(_, url = "example.com"))
      (1 to 3).foreach(_ => consumer ! RequestFactory(2L, url = "example.dev"))

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetSessionCountPerUrl, client.ref)
      client.expectMsg(SessionCountPerUrl(Map(
        "localhost" -> 6, "example.com" -> 4, "example.dev" -> 1
      )))
    }
  }

  "Querying for sessions per browser" should {
    "return the correct result with no trackers" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetSessionCountPerBrowser, client.ref)
      client.expectMsg(SessionCountPerBrowser(Map()))
    }

    "return the correct result with multiple trackers" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      (1 to 6).foreach(consumer ! RequestFactory(_))
      (1 to 4).foreach(consumer ! RequestFactory(_, browser = "firefox"))
      (1 to 3).foreach(_ => consumer ! RequestFactory(2L, browser = "safari"))

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetSessionCountPerBrowser, client.ref)
      client.expectMsg(SessionCountPerBrowser(Map(
        "chrome" -> 6, "firefox" -> 4, "safari" -> 1
      )))
    }
  }
}
