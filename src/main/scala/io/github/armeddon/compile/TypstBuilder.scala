package io.github.armeddon.compile

import cats.effect._
import cats.data.OptionT

import java.io.File
import java.nio.file.{Files, Paths, StandardOpenOption}

import io.github.armeddon.format.Format

object TypstBuilder {
  def build(
      content: String,
      format: Format
  ): IO[Option[Array[Byte]]] =
    (for {
      sourceFile <- createTempFileResource(sourcePrefix, sourceSuffix)
      targetFile <- createTempFileResource(targetPrefix, format.suffix)
    } yield (sourceFile, targetFile)).use { case (source, target) =>
      for {
        _ <- writeTypstCode(source, content, format)
        compiled <- compile(source, target, format)
      } yield compiled
    }

  private def createTempFileResource(
      prefix: String,
      suffix: String
  ): Resource[IO, File] =
    Resource.make(createTempFile(prefix, suffix))(file =>
      IO.blocking(file.delete).void
    )

  private def createTempFile(prefix: String, suffix: String): IO[File] =
    IO.blocking {
      val file = File.createTempFile(prefix, suffix)
      file.deleteOnExit()
      file
    }

  private def writeTypstCode(
      file: File,
      code: String,
      format: Format
  ): IO[Unit] =
    format match {
      case Format.HTML =>
        write(file, code)
      case _ => write(file, preamble ++ code)
    }

  private def write(file: File, content: String): IO[Unit] =
    IO.blocking {
      Files.writeString(file.toPath, content, StandardOpenOption.WRITE)
    }.void

  private def compile(
      source: File,
      target: File,
      format: Format
  ): IO[Option[Array[Byte]]] =
    createTypstProcessResource(source, target, format).use { process =>
      (for {
        exitCode <- OptionT.liftF(IO.blocking(process.waitFor()))
        contents <-
          if (exitCode == 0)
            OptionT.liftF(readFile(target))
          else
            OptionT.none[IO, Array[Byte]]
      } yield contents).value
    }

  private def createTypstProcessResource(
      source: File,
      target: File,
      format: Format
  ): Resource[IO, Process] =
    Resource.make {
      createTypstProcess(source, target, format)
    } {
      killProcess
    }

  private def createTypstProcess(
      source: File,
      target: File,
      format: Format
  ): IO[Process] =
    IO.blocking {
      new ProcessBuilder(
        "typst", "compile",
        source.getAbsolutePath(),
        target.getAbsolutePath(),
        s"--format=${format.format}",
        "--features=html"
      ).redirectErrorStream(true)
        .start()
    }

  private def killProcess(process: Process): IO[Unit] =
    IO.blocking {
      if (process.isAlive) process.destroy()
    }.void

  private def readFile(file: File): IO[Array[Byte]] =
    IO.blocking {
      Files.readAllBytes(Paths.get(file.toURI))
    }

  private lazy val sourcePrefix = "inline_typst_bot_source"
  private lazy val sourceSuffix = ".typ"
  private lazy val targetPrefix = "inline_typst_bot_target"

  private lazy val preamble =
    "#set page(height: auto, width: auto, margin: 5pt)\n"
}
