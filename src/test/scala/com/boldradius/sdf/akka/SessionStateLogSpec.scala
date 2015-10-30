package com.boldradius.sdf.akka

import akka.actor.{Props, ActorIdentity, Identify, ActorRef}
import akka.testkit.{TestActorRef, TestProbe, EventFilter}
import akka.util.Timeout
import com.boldradius.sdf.akka.Stats.SessionStats
import org.scalatest.concurrent.ScalaFutures
import akka.pattern.ask
import scala.concurrent.duration._

class SessionStateLogSpec extends BaseAkkaSpec  with ScalaFutures {


  "Sending Request to SessionStateLog" should {
    "result in list incrementing" in {
        val testConsumer =  TestProbe()
        val inactivetimeout = 2 seconds
        val sessionStateLog = system.actorOf(SessionStateLog.props(0,inactivetimeout), "sessionStateLog-list")
        sessionStateLog ! Request(0, 1, "url", "referrer", "browser")
        implicit val timeout:Timeout = Timeout(3 seconds)
        whenReady(  (sessionStateLog ? SessionStateLog.GetLog).mapTo[List[Request]] ){
          list => assert( list.contains(Request(0, 1, "url", "referrer", "browser")) )
        }
    }
  }

  "Not Sending Request to SessionStateLog within inactivetimeout" should {
    "result in SessionInactive msg" in {
      val inactivetimeout = 1 seconds

      val parent = TestProbe()
      val underTest = TestActorRef[SessionStateLog]( SessionStateLog.props(0, inactivetimeout), parent.ref, "child")


      val waitFor = 2 seconds
      val sessionStateLog = system.actorOf(SessionStateLog.props(0, inactivetimeout), "sessionStateLog-timeout")
      underTest ! Request(0, 1, "url", "referrer", "browser")
      parent.within(waitFor) {
        parent.expectMsg(Consumer.SessionInactive(0, List( Request(0, 1, "url", "referrer", "browser"))))
      }
    }
  }



  "Receving a request for /help" should {
    "result in creation of chat actor" in {
      val inactivetimeout = 20 seconds

      val parent = TestProbe()
      val underTest = system.actorOf(SessionStateLog.props(0, inactivetimeout), "session-chat")
      underTest ! Request(0, 1, "/help", "referrer", "browser")
      TestProbe().expectActor("/user/session-chat/chat-actor")
    }
  }


  "Receving a request for /help" should {
    "result in creation of chat actor that logs in 10 seconds if no other url comes in" in {
      val inactivetimeout = 20 seconds

      val parent = TestProbe()

      implicit val timeout:Timeout = Timeout(12 seconds)

      EventFilter.info(pattern = ".*Start Chat.*") intercept {
        val underTest = system.actorOf(SessionStateLog.props(30, inactivetimeout), "session-chat-2")
        underTest ! Request(0, 1, "/help", "referrer", "browser")
      }

    }
  }




}
