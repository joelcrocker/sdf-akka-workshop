package com.boldradius.sdf.akka

import akka.actor._
import scala.util.Random

/**
 * Created by lexcellent on 2015-11-06.
 */

object DenialOfServicer {
  def props(settings: DosSettings) = Props(new DenialOfServicer(settings))
  case class Start(dosTarget: ActorRef)
  case object SendOnce
}

class DosSession(maxId: Long) extends sim.Session {

  // These are consistent throughout the session
  override val id = Math.abs(Random.nextLong() % maxId)
}

class DenialOfServicer(settings: DosSettings) extends Actor with ActorLogging{
  import DenialOfServicer._

  import context.dispatcher

  def newRequest(session: sim.Session): Request = session.request

  override def receive: Receive = {
    case Start(dosTarget) =>
      log.info(s"Received a Start message with param ${dosTarget}")
      // do the dos'ing
      self ! SendOnce
      context.become(dosing(dosTarget))
  }

  def dosing(dosTarget: ActorRef): Receive = {
    case SendOnce => {
      //log.info(s"About to send a DOS message to ${dosTarget}")
      val newSession = new DosSession(settings.sessionIdMax)
      dosTarget ! newRequest(newSession)
      context.system.scheduler.scheduleOnce(settings.frequency, self, SendOnce)
    }
  }
  }
