package io.sciencebird.fs2poc

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.hikari._
import fs2.io.file.Files
import fs2._
import fs2.data.csv._

import java.nio.file.Paths
import java.util.Date
import java.text.SimpleDateFormat

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
    index_id CHAR(12),
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

  case class TableRow(
      symbol: String,
      at: Date,
      open: BigDecimal,
      high: BigDecimal,
      low: BigDecimal,
      close: BigDecimal,
      adjClose: BigDecimal,
      volume: BigDecimal,
      closeUsd: BigDecimal
  )

  object TableRow {

    //TODO: Fix this uggly parsing code
    def toTableRow: Pipe[IO, CsvRow[String], TableRow] = { in =>
      val format: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
      in.map { row =>
        row.values.toList match {
          case symbol :: at :: open :: high :: low :: close :: adjClose :: volume :: closeUsd :: Nil =>
            try {
              TableRow(
                symbol,
                format.parse(at),
                BigDecimal.apply(open),
                BigDecimal.apply(high),
                BigDecimal.apply(low),
                BigDecimal.apply(close),
                BigDecimal.apply(adjClose),
                BigDecimal.apply(volume),
                BigDecimal.apply(closeUsd)
              )
            } catch {
              case _: Throwable => throw new Exception(s"cannot parse row $row")
            }
          case _ => throw new Exception(s"cannot parse row $row")
        }
      }
    }
  }

  var insert: Update[TableRow] =
    Update[TableRow](
      s"""
        INSERT INTO sample_data (index_id,
          at,
          open,
          high,
          low,
          close,
          adj_close,
          volume,
          close_usd
        ) VALUES(?,?,?,?,?,?,?,?,?)
      """
    )

  def insertionPipe(xa: HikariTransactor[IO]): Pipe[IO, TableRow, Unit] = { in =>
    in
      .chunkN(4096)
      .flatMap { rows =>
        Stream.eval(insert.updateMany(rows).transact(xa).void)
      }
  }

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

    transactor.use { xa =>
      for {
        _ <- (drop, create).mapN(_ + _).transact(xa)
        recordCount <- Files[IO]
          .readAll(Paths.get("testdata/IndexProcessed.csv"), 4096)
          .through(text.utf8Decode)
          .through(text.lines)
          .compile
          .count
        start <- Clock[IO].monotonic
        _ <- Files[IO]
          .readAll(Paths.get("testdata/IndexProcessed.csv"), 4096)
          .through(text.utf8Decode)
          .through(lowlevel.rows[IO, String]())
          .through(lowlevel.headers[IO, String])
          .through(TableRow.toTableRow)
          .through(insertionPipe(xa))
          .compile
          .count
        end <- Clock[IO].monotonic
        _   <- IO.println(s"Inserted $recordCount records in ${(end - start).toMillis} ms")
      } yield ExitCode.Success
    }
  }

  override def run(args: List[String]): IO[ExitCode] = databaseProgram
}
