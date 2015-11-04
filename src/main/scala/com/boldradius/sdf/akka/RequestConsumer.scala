package com.boldradius.sdf.akka

import akka.actor._

object RequestConsumer {
  def props = Props(new RequestConsumer)
}

class RequestConsumer extends Actor with ActorLogging {
  def receive = {
    case request: Request =>
      log.info(s"RequestConsumer received a request $request.")
  }
}
