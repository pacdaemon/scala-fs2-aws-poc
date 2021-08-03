package io.sciencebird.fs2poc

import cats.effect._
import cats.implicits._
import doobie.implicits._
import doobie.hikari._
import doobie.util._
import fs2.data.csv.lowlevel
import fs2.io.file.Files
import fs2.text
import fs2.Pipe
import fs2._
import fs2.data.csv._
import fs2.data.csv.generic.semiauto._

import java.nio.file.Paths
import java.util.Date
import fs2.data.csv.CsvRowEncoder

object Fs2Poc extends IOApp {

  def countingProgram: IO[ExitCode] = {
    val s = Files[IO]
      .readAll(Paths.get("testdata/IndexProcessed.csv"), 4096)
      .through(text.utf8Decode)
      .through(text.lines)

    for {
      _      <- IO.println("Starting")
      result <- s.compile.count
      _      <- IO.println(s"Done $result lines")
    } yield ExitCode.Success

  }

  def lowLevelApiProgram: IO[ExitCode] = {

    val s = Files[IO]
      .readAll(Paths.get("testdata/IndexProcessed.csv"), 4096)
      .through(text.utf8Decode)
      .through(lowlevel.rows[IO, String]())
      .take(10)
      .through(lowlevel.headers[IO, String])

    for {
      _      <- IO.println("Starting")
      result <- s.compile.toList
      _      <- result.traverse(IO.println)
    } yield ExitCode.Success
  }

  val create = sql"""
    CREATE TABLE sample_data(
    index_id CHAR(3),
    at DATE,
    open NUMERIC(12,6),
    high NUMERIC(12,6),
    low NUMERIC(12,6),
    close NUMERIC(12,6),
    adj_close NUMERIC(12,6),
    volume NUMERIC(18,4),
    close_usd NUMERIC(20,10)
    )""".update.run

  val drop = sql"DROP TABLE IF EXISTS sample_data".update.run

  case class Row(
      indexId: String,
      at: Date,
      open: BigDecimal,
      high: BigDecimal,
      low: BigDecimal,
      close: BigDecimal,
      adjClose: BigDecimal,
      closeUds: BigDecimal
  )
  object Row {
    implicit val rowEncoder: CsvRowEncoder[Row, String] = deriveCsvRowEncoder
  }

  def insertionPipe: Pipe[IO, Row, Unit] = ???

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql:root",
        "root",
        "password",
        ce // await connection here
      )
    } yield xa

  def databaseProgram: IO[ExitCode] = {

    val s = Files[IO]
      .readAll(Paths.get("testdata/IndexProcessed.csv"), 4096)
      .through(text.utf8Decode)
      .through(lowlevel.rows[IO, String]())
      .take(10)
      .through(lowlevel.headers[IO, String])

    transactor.use { xa =>
      for {
        _     <- (drop, create).mapN(_ + _).transact(xa)
        elems <- s.compile.toList
        _     <- elems.traverse(IO.println)
      } yield ExitCode.Success
    }
  }

  override def run(args: List[String]): IO[ExitCode] = databaseProgram
}
