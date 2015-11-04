package com.boldradius.sdf.akka


object RequestFactory {

  def buildRequest(sessionId: Long) = Request(sessionId, System.currentTimeMillis(), "localhost", "google.com", "chrome")
}
