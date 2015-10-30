package com.boldradius.sdf.akka

import akka.actor.Actor
import akka.testkit.EventFilter
import akka.util.Timeout
import com.boldradius.sdf.akka.Stats.{GetRequestsPerBrowser, SessionStats}
import org.scalatest.concurrent.ScalaFutures
import akka.pattern.ask
import scala.concurrent.duration._
import scala.reflect.io.File

class StatsSpec extends BaseAkkaSpec  with ScalaFutures {


  override protected def beforeAll(): Unit = {
    File("target/snapshot-test").deleteRecursively()
  }


  "Collecting stats per session" should{
    "expose a GetRequestsPerBrowser aggregate" in{

      val statsActor = system.actorOf(Stats.props(10 seconds),"stats-request-browser")
      statsActor ! SessionStats(1, List(
      Request(0,0,"url","ref","browser1"),
        Request(0,0,"url","ref","browser1"),
          Request(0,0,"url","ref","browser2")
      ))

      statsActor ! SessionStats(1, List(
        Request(1,0,"url","ref","browser3")
      ))


      implicit val timeout:Timeout = Timeout(2 seconds)

      whenReady((statsActor ? GetRequestsPerBrowser).mapTo[Map[String,Int]])( map =>

        assert(map == Map( "browser1" -> 2, "browser2" -> 1,"browser3" -> 1 ))

      )
    }
  }


  "Stats actor" should{
    "periodically snapshot" in{

      EventFilter.info(pattern = ".*Saving Snapshot.*", occurrences = 1) intercept {
        val statsActor = system.actorOf(Stats.props(100 milliseconds),"stats-request-snapshot")
        statsActor ! SessionStats(1, List(
          Request(0,0,"url","ref","browser1"),
          Request(0,0,"url","ref","browser1"),
          Request(0,0,"url","ref","browser2")
        ))
      }

    }
  }


}
