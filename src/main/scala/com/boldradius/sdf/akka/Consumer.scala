package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.Consumer._
import com.boldradius.sdf.akka.RealTimeSessionStats.RunStats

import scala.concurrent.duration._


object Consumer {

  def props(inactiveTimeout:FiniteDuration):Props = Props(new Consumer(inactiveTimeout))

  case class SessionInactive(sessionId: Long, stats:List[Request])

  case object GetSessionTotal

  case object GetSessionsPerBrowser

  case object GetSessionsPerUrl

  case class SessionTotal(total:Int)


  def sessionsPerBrowser(requests:List[Request]):Map[String,Int] =
  requests.foldLeft(Map.empty[String,Int])((map, request) =>
    map + (request.browser -> (map.getOrElse(request.browser,0) + 1))
  )

  def sessionsPerUrl(requests:List[Request]):Map[String,Int] =
    requests.foldLeft(Map.empty[String,Int])((map, request) =>
      map + (request.url -> (map.getOrElse(request.url,0) + 1))
    )

  def reduction(list:List[Map[String,Int]]) = {
    list.reduce((a,b) =>  {

      // TODO use scalaz semigroup |+|

      val union = a.toList union b.toList
      union.groupBy(_._1 ).map{ case(k,v) => (k, v.map(_._2).sum)}
    })
  }

}

class Consumer(inactiveTimeout:FiniteDuration) extends Actor with ActorLogging{
  override def receive: Receive = withSessionLog(Map.empty)


  def withSessionLog(sessionlogMap: Map[Long, ActorRef]): Receive = {

    case r:Request =>
      log.info("Received Request")
      sessionlogMap.get(r.sessionId).fold[Unit]({
        val newSessionLogActor = context.actorOf(SessionStateLog.props(r.sessionId, inactiveTimeout))
        newSessionLogActor ! r
        context.become(withSessionLog( sessionlogMap + (r.sessionId -> newSessionLogActor) ))
      }
      )( sessionLogActorRef => sessionLogActorRef ! r )

    case SessionInactive(sessionId, stats) =>
      sessionlogMap.get(sessionId).fold[Unit]({}){sessionLogActorRef =>
       context.parent ! Stats.SessionStats(sessionId, stats)
       context.stop(sessionLogActorRef)
       context.become(withSessionLog(sessionlogMap - sessionId))

      }

    case GetSessionTotal => sender() ! SessionTotal(sessionlogMap.size)

    case GetSessionsPerBrowser =>
      val client  = sender()
      val real = createRealTimeActor(sessionlogMap.values.toList, client)
      real ! RunStats(Consumer.sessionsPerBrowser)



    case GetSessionsPerUrl =>
      val client  = sender()
      val real = createRealTimeActor(sessionlogMap.values.toList, client)
      real ! RunStats(Consumer.sessionsPerUrl)

  }

  def createRealTimeActor(sessions:List[ActorRef], client:ActorRef) =
    context.actorOf(RealTimeSessionStats.props(sessions, Consumer.reduction, client))


}
