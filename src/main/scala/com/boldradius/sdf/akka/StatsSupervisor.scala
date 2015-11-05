package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
 * Created by lexcellent on 11/5/15.
 */

object StatsSupervisor {
  def props(alerter: ActorRef, settings: ConsumerSettings) = {
    Props(new StatsSupervisor(alerter, settings))
  }

  case object GetStatsAggregator
  case class StatsAggregatorResponse(aggregator: ActorRef)
}

class StatsSupervisor(alerter: ActorRef, settings: ConsumerSettings)
  extends Actor with ActorLogging
{
  import StatsSupervisor._

  var lastThrowable: Option[Throwable] = None
  val statsAggregator = createStatsAggregator()

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
      maxNrOfRetries = settings.statsSupervisor.maxRetries,
      withinTimeRange = settings.statsSupervisor.retryTimeRange
    )(decider)
  }

  def createStatsAggregator(): ActorRef =
    context.actorOf(StatsAggregator.props(settings))
}
