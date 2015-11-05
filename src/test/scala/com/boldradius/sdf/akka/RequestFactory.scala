package com.boldradius.sdf.akka

object RequestFactory {
  def apply(sessionId: Long,
      timestamp: Long = System.currentTimeMillis(),
      url: String = "localhost",
      referrer: String = "google.com",
      browser: String = "chrome")
  : Request = {
    Request(sessionId, timestamp, url, referrer, browser)
  }

  def random(sessionId: Long, timeStamp: Long): Request = {
    Request(sessionId, timeStamp, sim.Session.randomUrl,
      sim.Session.randomReferrer, sim.Session.randomBrowser
    )
  }
}
