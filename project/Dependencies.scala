import sbt._

object Dependencies {
  lazy val scalaParsers = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  lazy val akkaActors = "com.typesafe.akka" %% "akka-actor" % "2.4.17"
  lazy val akkaHTTP = "com.typesafe.akka" %% "akka-http" % "10.0.4"
}
