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
}
