import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "BigPanda",
    libraryDependencies += scalaParsers,
    libraryDependencies += akkaActors,
    libraryDependencies += akkaHTTP
  )

cancelable in Global := true