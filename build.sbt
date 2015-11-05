name := """RequestProducer"""

version := "1.0"

scalaVersion := "2.11.7"

fork in Test := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"      % "2.3.14",
  "com.typesafe.akka" %% "akka-testkit"    % "2.3.14" % "test",
  "org.scalatest"     %% "scalatest"       % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-slf4j"      % "2.3.14",
  "com.typesafe.akka" %% "akka-testkit"    % "2.3.14",
  "ch.qos.logback"    %  "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.14",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
)
