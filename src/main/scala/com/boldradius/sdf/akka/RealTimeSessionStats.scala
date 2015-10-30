package com.boldradius.sdf.akka

import akka.actor.{Terminated, Props, ActorRef, Actor}
import com.boldradius.sdf.akka.RealTimeSessionStats.{RunStatsResponse, RunStats}
import akka.pattern.{ask,pipe}


object RealTimeSessionStats{

  def props(sessions:List[ActorRef],
               reduction:List[Map[String,Int]] => Map[String,Int],
                parent:ActorRef):Props = Props(new RealTimeSessionStats(sessions, reduction, parent))

  case class RunStats(f: List[Request] => Map[String,Int])

  case class RunStatsResponse(a:Map[String,Int])

}

class RealTimeSessionStats(sessions:List[ActorRef], reduction:List[Map[String,Int]] => Map[String,Int],  parent:ActorRef) extends Actor{


  override def receive: Receive = collect(0, sessions.size, Map.empty[String,Int])

  def collect(finishedCount:Int, totalSession:Int, finalResult:Map[String,Int]):Receive = {

    case msg:RunStats =>
      sessions.foreach{s =>
      context.watch(s)
      s ! msg
    }

    case RunStatsResponse(a) =>
      val newFinal = reduction(List(finalResult,a))
      val newFinishedCount = finishedCount + 1
      if( newFinishedCount == totalSession){
        parent ! RunStatsResponse(newFinal)
        context.stop(self)
      }else{
        context.become(collect(newFinishedCount, totalSession, newFinal))
      }

    case Terminated(a) => context.become(collect(finishedCount, totalSession - 1, finalResult))

  }

}
