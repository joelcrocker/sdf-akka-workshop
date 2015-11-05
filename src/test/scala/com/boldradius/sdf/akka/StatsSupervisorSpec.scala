package com.boldradius.sdf.akka

import akka.actor.{ActorLogging, Props}
import akka.actor._
import akka.pattern.ask
import akka.testkit._
import akka.util.Timeout
import com.boldradius.sdf.akka.StatsSupervisor.{StatsAggregatorResponse, GetStatsAggregator}
import scala.concurrent.Await
import scala.concurrent.duration._
import com.boldradius.sdf.akka.StatsAggregator.GetNumberOfRequestsPerBrowser
import org.scalatest.concurrent.ScalaFutures

case class SimulatedException() extends IllegalStateException("Simulated exception!")
class SimStatsAggregator extends Actor with ActorLogging {
  def receive: Receive = {
    case _ => throw SimulatedException()
  }
}

class StatsSupervisorSpec extends BaseAkkaSpec with ScalaFutures {
  "a StatsSupervisor with a child stats aggregator with a throwing receive" should {
    "restart the child a limited number of times, then let it die" in {
      implicit val timeout = Timeout(5 seconds)
      val probe = TestProbe()
      val supervisor = system.actorOf(Props(new StatsSupervisor() {
        override def createStatsAggregator() =
          context.actorOf(Props(new SimStatsAggregator()))
      }))

      val future = (supervisor ? GetStatsAggregator).mapTo[StatsAggregatorResponse]
      val StatsAggregatorResponse(aggregator) = Await.result(future, timeout.duration)

      probe.watch(aggregator)

      (1 to 5).map {
        _ => aggregator ! GetNumberOfRequestsPerBrowser
      }

      probe.expectTerminated(aggregator, 3 seconds)
    }
  }

  "restart the child while the number of restarts is less than the limit" in {
    implicit val timeout = Timeout(5 seconds)
    val probe = TestProbe()
    val supervisor = system.actorOf(Props(new StatsSupervisor() {
      override def createStatsAggregator() =
        context.actorOf(Props(new SimStatsAggregator()))
    }))

    val future = (supervisor ? GetStatsAggregator).mapTo[StatsAggregatorResponse]
    val StatsAggregatorResponse(aggregator) = Await.result(future, timeout.duration)

    probe.watch(aggregator)

    (1 to 2).map {
      _ => aggregator ! GetNumberOfRequestsPerBrowser
    }

    probe.expectNoMsg()
  }
}
