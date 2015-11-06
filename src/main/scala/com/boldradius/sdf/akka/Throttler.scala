package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._

object Throttler {
  case class Rejected(msg: Any)
  case object Tick

  sealed trait State
  private case object Idle extends State
  private case object Active extends State

  sealed trait Data
  private case class DataStore(remainingRequests: Int) extends Data

  def props(target: ActorRef, consumerSettings: ConsumerSettings) = {
    Props(new Throttler(target, consumerSettings))
  }
}


class Throttler(target: ActorRef, consumerSettings: ConsumerSettings)
  extends LoggingFSM[Throttler.State, Throttler.Data]
{
  import Throttler._
  val requestsPerSecond = consumerSettings.throttler.requestsPerSecond
  startWith(Idle, DataStore(requestsPerSecond))
  context.watch(target)

  val handleTerminated: StateFunction = {
    case Event(Terminated(`target`), _) =>
      context.stop(self)
      stay()
  }

  when(Idle)(handleTerminated orElse {
    case Event(msg, DataStore(remainingRequests)) =>
      target forward msg
      goto(Active) using DataStore(remainingRequests - 1)
  })

  when(Active)(handleTerminated orElse {
    case Event(Tick, DataStore(remaining)) if remaining != requestsPerSecond =>
      stay() using DataStore(requestsPerSecond)

    case Event(Tick, DataStore(remaining)) if remaining == requestsPerSecond =>
      goto(Idle) using DataStore(requestsPerSecond)

    case Event(msg, DataStore(remaining)) if remaining > 0 =>
      target forward msg
      stay() using DataStore(remaining - 1)

    case Event(msg, DataStore(remaining)) if remaining <= 0 =>
      sender() ! Rejected(msg)
      stay()
  })

  onTransition {
    case Idle -> Active => setTimer("ThrottlerTimer", Tick, 1.second, repeat = true)
    case Active -> Idle => cancelTimer("ThrottlerTimer")
  }
}
