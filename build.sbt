import Dependencies._

ThisBuild / scalaVersion     := "2.13.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.sciencebird"
ThisBuild / organizationName := "sciencebird"

lazy val root = (project in file("."))
  .settings(
    name                 := "scala-fs2-aws-poc",
    libraryDependencies ++= fs2.stack :+ catsEffect :+ fs2Data,
    libraryDependencies  += munit % Test
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
