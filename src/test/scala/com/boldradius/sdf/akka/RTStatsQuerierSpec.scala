package com.boldradius.sdf.akka

import akka.testkit._
import com.boldradius.sdf.akka.RTStatsQuerier._

class RTStatsQuerierSpec extends BaseAkkaSpec {
  // TODO: add tests for non-empty session counts.

  "Querying for total session count with no trackers" should {
    "return the correct result" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetTotalSessionCount, client.ref)
      client.expectMsg(TotalSessionCount(0))
    }
  }

  "Querying for sessions per url with no trackers" should {
    "return the correct result" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetSessionCountPerUrl, client.ref)
      client.expectMsg(SessionCountPerUrl(Map()))
    }
  }

  "Querying for sessions per browser with no trackers" should {
    "return the correct result" in {
      val consumer = system.actorOf(RequestConsumer.props(new ConsumerSettings()))
      val client = TestProbe()

      val querier = system.actorOf(RTStatsQuerier.props(consumer))
      querier.tell(GetSessionCountPerBrowser, client.ref)
      client.expectMsg(SessionCountPerBrowser(Map()))
    }
  }
}
