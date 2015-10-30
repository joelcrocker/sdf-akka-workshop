package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RealTimeSessionStats.{RunStatsResponse, RunStats}
import com.boldradius.sdf.akka.SessionStateLog.{Tick, GetLog}
import com.boldradius.sdf.chat.ChatActor
import scala.concurrent.duration._

object SessionStateLog {
  def props(sessionId: Long, inactiveTimeout:FiniteDuration):Props = Props(new SessionStateLog(sessionId,inactiveTimeout))

  case object GetLog

  case object Tick

}

class SessionStateLog(sessionId: Long, timeout:FiniteDuration) extends Actor with ActorLogging{
  override def receive: Receive = logRequests(List.empty,None, None)

  import context.dispatcher


  def logRequests(requestLog: List[Request], timer: Option[Cancellable], chatActor:Option[ActorRef]):Receive = {

    case r:Request =>
      // stop current timer if exists
      timer.fold({})(_.cancel())

      val newChatActor =
        if(r.url == "/help"){
        chatActor.fold( {
          Some(context.watch(context.actorOf(ChatActor.props, "chat-actor")))
        } )(ca => Some(ca))
      }else{
        chatActor.foreach( context.stop(_))
        None
      }
      context.become(logRequests(r :: requestLog, Some(startTimer),newChatActor))

    case GetLog => sender() ! requestLog


    case Tick =>
      context.parent ! Consumer.SessionInactive(sessionId, requestLog)

    case  RunStats(f) =>
      sender() ! RunStatsResponse(f(requestLog))

    case Terminated(c) => log.info("terminated " +c)



    case other => log.error(s"Unhandled msg: $other")


  }


  def startTimer = context.system.scheduler.scheduleOnce(timeout, self,Tick)


}
