package com.boldradius.sdf.akka

import akka.actor._

object Alerter {
  def props() = Props(new Alerter())

  case class Alarm(msg: String, ex: Throwable)
}

class Alerter extends Actor with ActorLogging {
  import Alerter._

  def receive: Receive = {
    case Alarm(msg, ex) => log.info(s"$msg. Exception: $ex")
  }
}
