package com.boldradius.sdf.akka

import akka.actor.Props
import akka.testkit._
import scala.concurrent.duration._

/**
 * Created by loutstanding on 2015-11-06.
 */
class DenialOfServicerSpec extends BaseAkkaSpec {
  val testRequest = RequestFactory(10L)
  class TestDenialOfServicer(settings: DosSettings) extends DenialOfServicer(settings) {
    override def newRequest(session: sim.Session): Request = testRequest
  }
  object TestDenialOfServicer {
    def props(settings: DosSettings) = Props(new TestDenialOfServicer(settings))
  }

  "DenialOfServicer actor" should {
    "Send requests " in {
      // set up a dummy consumer
      val dosSettings = new DosSettings(config)

      val consumerProbe = TestProbe()
      val doser = system.actorOf(TestDenialOfServicer.props(dosSettings), "dos")

      // pass that to DOSer w/ Start message
      doser ! DenialOfServicer.Start(consumerProbe.ref)

      // expect a message within x seconds
      consumerProbe.within(200 milliseconds) {
        consumerProbe.expectMsg(testRequest)
      }
    }
  }
}
