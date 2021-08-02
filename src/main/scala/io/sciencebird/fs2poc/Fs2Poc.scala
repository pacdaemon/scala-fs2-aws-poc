package io.sciencebird.fs2poc

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import fs2.io.file.Files
import java.nio.file.Paths
import fs2.text
import fs2.data.csv.lowlevel

object Fs2Poc extends IOApp {

  def countingProgram: IO[ExitCode] = {
    val s = Files[IO]
      .readAll(Paths.get("testdata/IndexData.csv"), 4096)
      .through(text.utf8Decode)
      .through(text.lines)

    for {
      _      <- IO.println("Starting")
      result <- s.compile.count
      _      <- IO.println(s"Done $result lines")
    } yield ExitCode.Success

  }

  def lowLevelApi: IO[ExitCode] = {

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

  override def run(args: List[String]): IO[ExitCode] = lowLevelApi
}
