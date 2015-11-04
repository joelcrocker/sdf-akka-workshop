package com.boldradius.sdf.akka

import akka.testkit._
import scala.concurrent.duration._

class SessionTrackerSpec extends BaseAkkaSpec {
  "A new Request" should {
    "Result in a Request being added to the session history" in {
      val actorRef = TestActorRef(new SessionTracker(100L, 20 seconds, system.deadLetters))
      val request = RequestFactory(sessionId = 100L)
      val request2 = RequestFactory(sessionId = 100L)
      val prevSize = actorRef.underlyingActor.history.size
      actorRef ! request
      actorRef ! request2
      val newSize = actorRef.underlyingActor.history.size

      newSize shouldEqual prevSize + 2
      actorRef.underlyingActor.history.last shouldEqual request2
    }
  }

  "Inactivity" should {
    "Result in stats message being sent to the stats collector" in {
      val statsProbe = TestProbe()
      val sessionId = 500L
      val request = RequestFactory(sessionId = sessionId)

      val tracker = system.actorOf(SessionTracker.props(sessionId, 50 milliseconds, statsProbe.ref))
      tracker ! request

      statsProbe.within(50 milliseconds, 200 milliseconds) {
        statsProbe.expectMsg(
          SessionTracker.SessionStats(sessionId, Seq(request))
        )
      }
    }

    "Result in the tracker being stopped" in {
      val probe = TestProbe()

      val sessionId = 500L
      val request = RequestFactory(sessionId = sessionId)

      val tracker = system.actorOf(SessionTracker.props(sessionId, 50 milliseconds, system.deadLetters))
      probe.watch(tracker)

      tracker ! request
      probe.expectTerminated(tracker)
    }
  }
}
