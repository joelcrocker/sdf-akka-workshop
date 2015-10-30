package com.boldradius.sdf.chat

import akka.actor.{ActorLogging, Actor, Props}
import com.boldradius.sdf.chat.ChatActor.InitiateChat
import scala.concurrent.duration._

object ChatActor{
  def props:Props = Props(new ChatActor)

  case object InitiateChat
}

class ChatActor extends Actor with ActorLogging{

  import context.dispatcher

  println("started " + self)

  context.system.scheduler.scheduleOnce(10 seconds, self, InitiateChat)

  override def receive: Receive = {

    case InitiateChat =>
      println("***********************************  InitiateChat")
      log.info("Start Chat")
      context.stop(self)

  }
}
