package com.boldradius.sdf.akka

import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Inspectors, Matchers, WordSpecLike}
import scala.collection.JavaConversions._


class ThrottlerSpec extends TestKit(ActorSystem()) with ImplicitSender
  with WordSpecLike with Matchers with Inspectors with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    shutdown()
  }

  val settings = new ConsumerSettings(ConfigFactory.parseMap(
    Map("web-stats.throttler.requests-per-second" -> "2")
  ).withFallback(ConfigFactory.load()))
  assert(settings.throttler.requestsPerSecond == 2)

  "Throttler" should {
    "pass all messages under limit" in {
      val probe = TestProbe()
      val throttler = system.actorOf(Throttler.props(probe.ref, settings))
      throttler ! "testMsg1"
      probe.expectMsg("testMsg1")
      throttler ! "testMsg2"
      probe.expectMsg("testMsg2")
    }

    "reject all messages over limit" in {
      val probe = TestProbe()
      val throttler = system.actorOf(Throttler.props(probe.ref, settings))
      for (i <- 1 to 2) {
        val msg = s"testMsg$i"
        throttler ! msg
        probe.expectMsg(msg)
      }
      for (i <- 3 to 4) {
        val msg = s"testMsg$i"
        throttler ! msg
        expectMsg(Throttler.Rejected(msg))
      }
    }
  }
}
