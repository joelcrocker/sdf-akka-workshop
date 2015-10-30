package com.boldradius.sdf.email

import akka.actor.{Actor, ActorLogging, Props}
import com.boldradius.sdf.email.EmailActor.StatsDown


object EmailActor{

  def props:Props = Props(new EmailActor)

  case object StatsDown

}

class EmailActor extends Actor with ActorLogging{

  override def receive: Receive ={

    case StatsDown =>
      println("*************  EmailActor StatsDown")
      log.info("Stats down")


  }
}
