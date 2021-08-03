import sbt._

object Dependencies {

  object Versions {
    lazy val catsEffect = "3.1.1"
    lazy val doobie     = "1.0.0-M5"
    lazy val fs2        = "3.0.6"
    lazy val fs2Aws     = "4.0.0-RC2"
    lazy val fs2Data    = "1.0.0"
    lazy val hiraki     = "3.3.0"
    lazy val munit      = "0.7.27"
  }

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

  object fs2 {
    lazy val fs2Core     = "co.fs2" %% "fs2-core"             % Versions.fs2
    lazy val fs2Io       = "co.fs2" %% "fs2-io"               % Versions.fs2
    lazy val fs2Reactive = "co.fs2" %% "fs2-reactive-streams" % Versions.fs2

    lazy val stack = fs2Core :: fs2Io :: fs2Reactive :: Nil
  }

  object doobie {
    // Start with this one
    lazy val core = "org.tpolecat" %% "doobie-core" % Versions.doobie

    // And add any of these as needed
    lazy val h2     = "org.tpolecat" %% "doobie-h2"     % Versions.doobie // H2 driver 1.4.200 + type mappings.
    lazy val hiraki = "org.tpolecat" %% "doobie-hikari" % Versions.doobie // HikariCP transactor.
    lazy val postgres =
      "org.tpolecat" %% "doobie-postgres" % Versions.doobie // Postgres driver 42.2.19 + type mappings.
    //"org.tpolecat" %% "doobie-quill"     % "0.12.1",          // Support for Quill 3.6.1
    //"org.tpolecat" %% "doobie-specs2"    % "0.12.1" % "test", // Specs2 support for typechecking statements.
    //"org.tpolecat" %% "doobie-scalatest" % "0.12.1" % "test"  // ScalaTest support for typechecking statements.

    lazy val stack = core :: hiraki :: postgres :: Nil

  }

  lazy val fs2Aws = "io.laserdisc" %% "fs2-aws" % Versions.fs2Aws

  lazy val munit = "org.scalameta" %% "munit" % Versions.munit

  object fs2Data {
    lazy val csv        = "org.gnieh" %% "fs2-data-csv"         % Versions.fs2Data
    lazy val csvGeneric = "org.gnieh" %% "fs2-data-csv-generic" % Versions.fs2Data

    lazy val stack = csv :: csvGeneric :: Nil
  }
}
