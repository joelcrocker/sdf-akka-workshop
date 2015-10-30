package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RequestProducer.Start
import com.boldradius.sdf.akka.SimulationActor.StartApp
import com.boldradius.sdf.email.EmailActor

import scala.concurrent.duration._

object SimulationActor{
  case object StopApp
  case object StartApp

}

class SimulationActor extends Actor with ActorLogging{

  val inactiveTimeout:FiniteDuration = FiniteDuration(context.system.settings.config.getDuration("session.inactive", SECONDS),SECONDS)
  val email = createEmail
  val consumer = createConsumer
  val producer = createProducer
  val statsActor = createStatsActor

  var statsRetries = 0

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case Stats.SimulatedException =>
        if(statsRetries < 2){
          statsRetries += 1
          SupervisorStrategy.Restart
        }else{
          email ! EmailActor.StatsDown
          SupervisorStrategy.Stop
        }

      case other =>
        SupervisorStrategy.Restart
    }
    OneForOneStrategy()(decider orElse super.supervisorStrategy.decider)
  }


  def handleStats: Receive = {
    case msg:Stats.SessionStats =>  statsActor.forward(msg)

    case other => log.info("Unhandled: " + other)
  }


  def boot:Receive = {
    case StartApp =>
      producer ! Start(consumer)
  }

  def receive: Receive = boot orElse handleStats


  def createProducer:ActorRef ={
    context.actorOf(RequestProducer.props(100), "producerActor")
  }


  def createConsumer:ActorRef = context.actorOf(Consumer.props(inactiveTimeout), "consumerActor")

  def createStatsActor:ActorRef ={
    context.actorOf(Stats.props, "statsActor")
  }

  def createEmail:ActorRef = context.actorOf(EmailActor.props, "emailActor")

}

