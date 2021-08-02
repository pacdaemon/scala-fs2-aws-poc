package io.sciencebird.fs2poc

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import fs2.io.file.Files
import java.nio.file.Paths
import fs2.text

object Fs2Poc extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
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

}
