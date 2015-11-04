package com.boldradius.sdf.akka


object RequestFactory {
  def apply(sessionId: Long): Request = {
    Request(sessionId, System.currentTimeMillis(), "localhost", "google.com", "chrome")
  }
}
