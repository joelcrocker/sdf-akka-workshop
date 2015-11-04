package com.boldradius.sdf.akka

import akka.testkit._

class SessionTrackerSpec extends BaseAkkaSpec {
  "A new Request" should {
    "Result in a Request being added to the session history" in {
      val actorRef = TestActorRef(new SessionTracker(100L))
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
}
