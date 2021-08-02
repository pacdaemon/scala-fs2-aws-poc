import sbt._

object Dependencies {

  object Versions {
    lazy val catsEffect = "3.1.1"
    lazy val fs2        = "3.0.6"
    lazy val fs2Aws     = "4.0.0-RC2"
    lazy val fs2Data    = "1.0.0"
    lazy val munit      = "0.7.27"
  }

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

  object fs2 {
    lazy val fs2Core     = "co.fs2" %% "fs2-core"             % Versions.fs2
    lazy val fs2Io       = "co.fs2" %% "fs2-io"               % Versions.fs2
    lazy val fs2Reactive = "co.fs2" %% "fs2-reactive-streams" % Versions.fs2

    lazy val stack = fs2Core :: fs2Io :: fs2Reactive :: Nil
  }

  lazy val fs2Aws = "io.laserdisc" %% "fs2-aws" % Versions.fs2Aws

  lazy val munit = "org.scalameta" %% "munit" % Versions.munit

  lazy val fs2Data = "org.gnieh" %% "fs2-data-csv" % Versions.fs2Data
}
