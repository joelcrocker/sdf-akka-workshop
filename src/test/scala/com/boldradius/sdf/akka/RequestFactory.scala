package com.boldradius.sdf.akka


object RequestFactory {
  def apply(sessionId: Long, timestamp: Long = System.currentTimeMillis()): Request = {
    Request(sessionId, timestamp, "localhost", "google.com", "chrome")
  }
}
