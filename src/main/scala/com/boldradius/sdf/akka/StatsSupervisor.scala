package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
 * Created by lexcellent on 11/5/15.
 */

object StatsSupervisor {
  def props() = Props(new StatsSupervisor())

  case object GetStatsAggregator
  case class StatsAggregatorResponse(aggregator: ActorRef)
}

class StatsSupervisor extends Actor with ActorLogging {
  import StatsSupervisor._

  var lastThrowable: Option[Throwable] = None
  val statsAggregator = createStatsAggregator()
  val alerter = context.actorOf(Alerter.props)

  context.watch(statsAggregator)

  def receive: Receive = {
    case Terminated(aggregator) =>
      alerter ! Alerter.Alarm(
        s"StatsAggregator $statsAggregator hit retry limit and has been stopped.",
        lastThrowable.get
      )

    case GetStatsAggregator =>
      sender ! StatsAggregatorResponse(statsAggregator)
  }

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case NonFatal(e) => {
        log.debug(s"Supervisor received throwable $e")
        lastThrowable = Some(e)
        SupervisorStrategy.Restart
      }
    }
    OneForOneStrategy(
      maxNrOfRetries = 2, withinTimeRange = 15 minutes
    )(decider)
  }

  def createStatsAggregator(): ActorRef =
    context.actorOf(StatsAggregator.props)
}
