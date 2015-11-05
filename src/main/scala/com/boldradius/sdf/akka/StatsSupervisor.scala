package com.boldradius.sdf.akka

import akka.actor._
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
 * Created by lexcellent on 11/5/15.
 */

object StatsSupervisor {
  val defaultMaxRetries: Int = 2
  val defaultRetryTimeRange: Duration = 15 minutes

  def props(alerter: ActorRef,
            maxRetries: Int = defaultMaxRetries,
            retryTimeRange: Duration = defaultRetryTimeRange) = {
    Props(new StatsSupervisor(alerter, maxRetries, retryTimeRange))
  }

  case object GetStatsAggregator
  case class StatsAggregatorResponse(aggregator: ActorRef)
}

class StatsSupervisor(alerter: ActorRef,
                      maxRetries: Int = StatsSupervisor.defaultMaxRetries,
                      retryTimeRange: Duration = StatsSupervisor.defaultRetryTimeRange
) extends Actor with ActorLogging {
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
      maxNrOfRetries = maxRetries, withinTimeRange = retryTimeRange
    )(decider)
  }

  def createStatsAggregator(): ActorRef =
    context.actorOf(StatsAggregator.props)
}
