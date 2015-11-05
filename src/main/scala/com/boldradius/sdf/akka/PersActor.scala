package com.boldradius.sdf.akka

import akka.actor._
import akka.persistence.{SnapshotOffer, PersistentActor}

object PersActor {
  case class Event(msg: String)
  case class Command(msg: String)
}

class PersActor extends PersistentActor {
  import PersActor._
  override def persistenceId: String = "pers-actor"
  val snapshotInterval = 100

  var hist = IndexedSeq.empty[String]

  def updateState(event: Event): Unit = {
    hist :+= event.msg
  }

  override def receiveRecover: Receive = {
    case e: Event =>
      updateState(e)

    case SnapshotOffer(_, snapshot: IndexedSeq[String]) =>
      hist = snapshot
  }

  override def receiveCommand: Receive = {
    case Command(msg) =>
      val event = Event(msg)
      persist(event)(updateState)
      if (hist.size % snapshotInterval == 0) {
        saveSnapshot(hist)
      }
  }
}
