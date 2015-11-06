package com.boldradius.sdf.akka

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest._
import scala.concurrent.duration._
import scala.reflect.io.File
import scala.collection.JavaConversions._

class StatsAggregatorSpec extends BaseAkkaSpec {

  "StatsAggregator" should {
    "compute stats per browser" in {
      val sessionId = 100L
      def mkRequest(browser: String) = {
        Request(sessionId, System.currentTimeMillis(), sim.Session.randomUrl,
          sim.Session.randomReferrer, browser
        )
      }
      val oldStats = StatsAggregator.BrowserStats(
        requests = Map("chrome" -> 100),
        users = Map("chrome" -> 5)
      )
      val sessionHistory = {
        (0 until 5).map(_ => mkRequest("chrome")) ++
          (0 until 7).map(_ => mkRequest("firefox"))
      }
      val newStats = StatsAggregator.statsPerBrowser(oldStats, sessionHistory)
      newStats.requests shouldBe Map("chrome" -> 105, "firefox" -> 7)
      newStats.users shouldBe Map("chrome" -> 6, "firefox" -> 1)
    }

    "compute visit stats per url" in {
      import StatsAggregator.UrlVisitStats
      val sessionId = 100L
      def mkRequest(url: String, time: Long) = {
        Request(sessionId, time, url, sim.Session.randomReferrer, sim.Session.randomBrowser)
      }
      val oldStats = Map("url1" -> UrlVisitStats(100, 4))
      // url1 gets 1 visit with 10 ms total, url2 gets 2 visits, with 60 ms total
      val sessionHistory = Seq(
        mkRequest("url1", 1000),
        mkRequest("url2", 1010), // 10 ms
        mkRequest("url2", 1030), // 20 ms
        mkRequest("url1", 1070) // 40 ms
      )
      val newStats = StatsAggregator.statsVisitsPerUrl(oldStats, sessionHistory)
      newStats shouldBe Map("url1" -> UrlVisitStats(110, 5), "url2" -> UrlVisitStats(60, 2))
    }
  }

  "Referrers per user" should {
    "be updated correctly" in {
      val sessionId = 100L
      def mkRequest(referrer: String) = {
        Request(sessionId, System.currentTimeMillis(), sim.Session.randomUrl,
          referrer, sim.Session.randomBrowser
        )
      }
      val oldStats = StatsAggregator.ReferrerStats(
        Map("google" -> 5, "facebook" -> 3, "twitter" -> 1)
      )
      val sessionHistory = {
        (0 until 5).map(_ => mkRequest("facebook")) ++
          (0 until 7).map(_ => mkRequest("google"))
      }

      val newStats = StatsAggregator.statsPerReferrer(oldStats, sessionHistory)
      newStats.referrers shouldBe Map("google" -> 12, "facebook" -> 8, "twitter" -> 1)
    }

    "return the top two referrers by user" in {
      val stats = StatsAggregator.ReferrerStats(
        Map("google" -> 5, "facebook" -> 3, "twitter" -> 1)
      )

      val topTwo = stats.topReferrers(2)
      topTwo shouldBe Seq(("google", 5), ("facebook", 3))
    }
  }

  "Busiest request per minute" should {
    "return the expected value" in {
      val oldStats = Map(50L -> 100L, 60L -> 200L)

      val requestHistory = Seq(
        RequestFactory(sessionId = 500L, timestamp = (1 minute).toMillis),
        RequestFactory(sessionId = 600L, timestamp = (5 minute).toMillis),
        RequestFactory(sessionId = 600L, timestamp = (5 minute).toMillis),
        RequestFactory(sessionId = 600L, timestamp = (5 minute).toMillis),
        RequestFactory(sessionId = 600L, timestamp = (5 minute).toMillis)
      )

      val newStats = StatsAggregator.updatedRequestsPerMinute(oldStats, requestHistory)

      val busiestMinute = StatsAggregator.busiestMinute(newStats)
      busiestMinute shouldEqual StatsAggregator.ResBusiestMinute(60, 200L)
    }
  }

  "StatsAggregator" should {
    "compute stats per url" should {
      val oldStats = StatsAggregator.UrlStats(
        countByUrl = Map("yahoo.com" -> 10, "boldradius.com" -> 5, "leveloflexcellence.com" -> 1)
      )
      val sessionHistory = Seq(
        RequestFactory(100L, url = "yahoo.com"),
        RequestFactory(100L, url = "gmail.com"),
        RequestFactory(100L, url = "hotmail.com"),
        RequestFactory(100L, url = "pagerduty.com")
      )
      val newStats = StatsAggregator.countByPage(oldStats, sessionHistory)

      "overall url counts" in {
        newStats.urlCount("yahoo.com") shouldEqual 11
        newStats.urlCount("pagerduty.com") shouldEqual 1
      }

      "urls by percentage" in {
        newStats.urlPercentage("yahoo.com") shouldEqual 55
        newStats.urlPercentage("pagerduty.com") shouldEqual 5
      }
    }

    "compute count per sink correctly" in {
      val oldSinkStats = StatsAggregator.UrlStats(
        countByUrl = Map("gmail.com/logout" -> 10, "boldradius.com/logout" -> 5)
      )
      val sessionHistory = Seq(
        RequestFactory(100L, url = "boldradius.com/logout"),
        RequestFactory(100L, url = "hotmail.com"),
        RequestFactory(100L, url = "gmail.com/logout")
      )
      val newSinkStats = StatsAggregator.countPerSink(oldSinkStats, sessionHistory)
      newSinkStats.urlCount("gmail.com/logout") shouldEqual 11
      newSinkStats.urlCount("boldradius.com/logout") shouldEqual 5
    }

    "compute count per landing correctly" in {
      val oldLandingStats = StatsAggregator.UrlStats(
        countByUrl = Map("gmail.com/logout" -> 10, "boldradius.com/logout" -> 5)
      )
      val sessionHistory = Seq(
        RequestFactory(100L, url = "boldradius.com/logout"),
        RequestFactory(100L, url = "hotmail.com"),
        RequestFactory(100L, url = "gmail.com/logout")
      )
      val newLandingStats = StatsAggregator.countPerLanding(oldLandingStats, sessionHistory)
      newLandingStats.urlCount("gmail.com/logout") shouldEqual 10
      newLandingStats.urlCount("boldradius.com/logout") shouldEqual 6
    }
  }
}

class StatsAggregatorMessageSpec extends TestKit(ActorSystem()) with ImplicitSender
  with WordSpecLike with Matchers with Inspectors with BeforeAndAfterAll
{
  val config = ConfigFactory.load()
  val settings = new ConsumerSettings(config)
  override def beforeAll(): Unit = {
    val config = system.settings.config
    File(config.getString("akka.persistence.journal.leveldb.dir")).deleteRecursively()
    File(config.getString("akka.persistence.snapshot-store.local.dir")).deleteRecursively()
  }
  override def afterAll(): Unit = {
    shutdown()
  }
  def uniqueName(): String = {
    System.identityHashCode(this) + "-" +
      Thread.currentThread().getId.toString + "-" +
      System.nanoTime().toString
  }
  def mkAggregatorActor(): ActorRef = {
    system.actorOf(StatsAggregator.props(settings), uniqueName())
  }

  "StatsAggregator actor" should {
    import StatsAggregator._
    val sessionId = 100L

    "respond to GetNumberOfRequestsPerBrowser" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(browser = "chrome"),
        RequestFactory.random(sessionId, 20).copy(browser = "chrome"),
        RequestFactory.random(sessionId, 30).copy(browser = "firefox")
      ))
      aggregator ! GetNumberOfRequestsPerBrowser
      expectMsg(ResNumberOfRequestsPerBrowser(Map("chrome" -> 2L, "firefox" -> 1L)))
    }

    "respond to GetBusiestMinute" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(browser = "chrome"),
        RequestFactory.random(sessionId, 60020).copy(browser = "chrome"),
        RequestFactory.random(sessionId, 60030).copy(browser = "firefox")
      ))
      aggregator ! GetBusiestMinute
      expectMsg(ResBusiestMinute(1, 2L))
    }

    "respond to GetPageVisitDistribution" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(url = "google.com"),
        RequestFactory.random(sessionId, 20).copy(url = "google.com"),
        RequestFactory.random(sessionId, 30).copy(url = "google.ca")
      ))
      aggregator ! GetPageVisitDistribution
      expectMsg(ResPageVisitDistribution(Map("google.com" -> 2.0/3, "google.ca" -> 1.0/3)))
    }

    "respond to GetAverageVisitTimePerUrl" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(url = "google.com"),
        RequestFactory.random(sessionId, 30).copy(url = "google.com"),
        RequestFactory.random(sessionId, 40).copy(url = "google.ca"),
        RequestFactory.random(sessionId, 60).copy(url = "google.ca")
      ))
      aggregator ! GetAverageVisitTimePerUrl
      expectMsg(ResAverageVisitTimePerUrl(Map("google.com" -> 15.0, "google.ca" -> 20.0)))
    }

    "respond to GetTopLandingPages" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(url = "google.com"),
        RequestFactory.random(sessionId, 20).copy(url = "google.ca")
      ))
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId + 1, 10).copy(url = "google.com"),
        RequestFactory.random(sessionId + 1, 20).copy(url = "google.ca")
      ))
      aggregator ! GetTopLandingPages
      expectMsg(ResTopLandingPages(Seq("google.com" -> 2)))
    }

    "respond to GetTopSinkPages" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(url = "google.com"),
        RequestFactory.random(sessionId, 20).copy(url = "google.ca")
      ))
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId + 1, 10).copy(url = "google.com"),
        RequestFactory.random(sessionId + 1, 20).copy(url = "google.ca")
      ))
      aggregator ! GetTopSinkPages
      expectMsg(ResTopSinkPages(Seq("google.ca" -> 2)))
    }

    "respond to GetTopBrowsers" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(browser = "chrome"),
        RequestFactory.random(sessionId, 20).copy(browser = "chrome"),
        RequestFactory.random(sessionId, 30).copy(browser = "firefox")
      ))
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId + 1, 10).copy(browser = "chrome"),
        RequestFactory.random(sessionId + 1, 20).copy(browser = "chrome")
      ))
      aggregator ! GetTopBrowsers
      expectMsg(ResTopBrowsers(Seq("chrome" -> 2L, "firefox" -> 1L)))
    }

    "respond to GetTopReferrers" in {
      val aggregator = mkAggregatorActor()
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(referrer = "google.com"),
        RequestFactory.random(sessionId, 30).copy(referrer = "google.com"),
        RequestFactory.random(sessionId, 40).copy(referrer = "google.ca"),
        RequestFactory.random(sessionId, 50).copy(referrer = "google.ca"),
        RequestFactory.random(sessionId, 60).copy(referrer = "google.fr"),
        RequestFactory.random(sessionId, 60).copy(referrer = "google.com")
      ))
      aggregator ! GetTopReferrers
      expectMsg(ResTopReferrers(Seq("google.com" -> 3, "google.ca" -> 2)))
    }
  }
}

class StatsAggregatorPersistenceSpec extends TestKit(ActorSystem()) with ImplicitSender
  with WordSpecLike with Matchers with Inspectors with BeforeAndAfterAll
{
  override def beforeAll(): Unit = {
    val config = system.settings.config
    File(config.getString("akka.persistence.journal.leveldb.dir")).deleteRecursively()
    File(config.getString("akka.persistence.snapshot-store.local.dir")).deleteRecursively()
  }
  override def afterAll(): Unit = {
    shutdown()
  }
  def uniqueName(): String = {
    System.identityHashCode(this) + "-" +
      Thread.currentThread().getId.toString + "-" +
      System.nanoTime().toString
  }
  def mkAggregatorActor(name: String): ActorRef = {
    system.actorOf(StatsAggregator.props(settings), name)
  }
  val settings = new Settings(ConfigFactory.parseMap(
    Map("web-stats.stats-aggregator.snapshot-interval" -> "2")
  ).withFallback(ConfigFactory.load()))
  assert(settings.statsAggregator.snapshotInterval == 2)

  "StatsAggregator persistent actor" should {
    import StatsAggregator._
    val sessionId = 100L

    "start with empty state when run the first time" in {
      val aggregator = mkAggregatorActor(uniqueName())
      aggregator ! GetNumberOfRequestsPerBrowser
      expectMsg(ResNumberOfRequestsPerBrowser(Map.empty))
    }

    "reload state from journal on startup" in {
      val persistentName = uniqueName()
      val aggregator = mkAggregatorActor(persistentName)
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(browser = "chrome")
      ))
      aggregator ! GetNumberOfRequestsPerBrowser
      expectMsg(ResNumberOfRequestsPerBrowser(Map("chrome" -> 1L)))
      watch(aggregator)
      system.stop(aggregator)
      expectTerminated(aggregator)

      val newAggregator = mkAggregatorActor(persistentName)
      newAggregator ! GetNumberOfRequestsPerBrowser
      expectMsg(ResNumberOfRequestsPerBrowser(Map("chrome" -> 1L)))
    }

    "reload state from snapshot on startup" in {
      val persistentName = uniqueName()
      val aggregator = mkAggregatorActor(persistentName)
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId, 10).copy(browser = "chrome")
      ))
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId + 1, 10).copy(browser = "chrome")
      ))
      aggregator ! SessionTracker.SessionStats(sessionId, Seq(
        RequestFactory.random(sessionId + 2, 10).copy(browser = "chrome")
      ))
      aggregator ! GetNumberOfRequestsPerBrowser
      expectMsg(ResNumberOfRequestsPerBrowser(Map("chrome" -> 3L)))
      watch(aggregator)
      system.stop(aggregator)
      expectTerminated(aggregator)

      val newAggregator = mkAggregatorActor(persistentName)
      newAggregator ! GetNumberOfRequestsPerBrowser
      expectMsg(ResNumberOfRequestsPerBrowser(Map("chrome" -> 3L)))
    }
  }
}
