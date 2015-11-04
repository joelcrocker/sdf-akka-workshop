package com.boldradius.sdf.akka

case class Request(sessionId: Long, timestamp: Long, url: String, referrer: String, browser: String)
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
