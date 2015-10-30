package com.boldradius.sdf.akka

import akka.actor.{ActorRef, Props}
import akka.testkit.{EventFilter, TestProbe}
import com.boldradius.sdf.akka.Stats.{SimulatedException, SessionStats}
import scala.concurrent.duration._

class SimulationActorSpec  extends BaseAkkaSpec {

  "Creating SimulationActor" should {

    "result in creating a top-level actors named 'producerActor' and 'consumerActor' and 'statsActor' " in {
      val sim = system.actorOf(Props(new SimulationActor), "sim-actor")
      sim  ! SessionStats(0, Nil)
      TestProbe().expectActor("/user/sim-actor/producerActor")
      TestProbe().expectActor("/user/sim-actor/consumerActor")
      TestProbe().expectActor("/user/sim-actor/statsActor")
      TestProbe().expectActor("/user/sim-actor/emailActor")
    }

    "Cause a write to the email log after 2 tries" in {


      val simemail = system.actorOf(Props(new SimulationActor{
        override   def createStatsActor:ActorRef ={
          context.actorOf(Props(new Stats(10 seconds){
              override def receive:Receive = {
                case _ => throw SimulatedException
              }
          }), "statsActor")
        }

      }), "sim-actor-email")

      EventFilter.info(pattern = ".*Stats down.*", occurrences = 1) intercept {
        simemail ! SessionStats(0, Nil)
        simemail ! SessionStats(0, Nil)
        simemail ! SessionStats(0, Nil)
      }

    }

    }






}
