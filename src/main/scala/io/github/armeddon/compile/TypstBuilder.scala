package io.github.armeddon.compile

import cats.effect._
import cats.data.EitherT

import org.typelevel.log4cats.Logger

import java.io.File
import java.nio.file.{Files, Paths, StandardOpenOption}

import io.github.armeddon.format.Format

object TypstBuilder {
  def build(
      content: String,
      format: Format
  )(implicit logger: Logger[IO]): IO[Either[String, Array[Byte]]] =
    (for {
      sourceFile <- createTempFileResource(sourcePrefix, sourceSuffix)
      targetFile <- createTempFileResource(targetPrefix, format.suffix)
    } yield (sourceFile, targetFile)).use { case (source, target) =>
      for {
        _ <- logger.info("Created source and target files.")
        _ <- logger.info("Writing Typst code into the source file.")
        _ <- writeTypstCode(source, content, format)
        _ <- logger.info("Typst code written into the source file.")
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
  )(implicit logger: Logger[IO]): IO[Either[String, Array[Byte]]] =
    createTypstProcessResource(source, target, format).use { process =>
      (for {
        exitCode <- EitherT.right[String](IO.blocking(process.waitFor()))
        _ <- EitherT.right[String] {
          if (exitCode == 0) logger.info("Typst compiled successfully.")
          else logger.error("Typst compilation failed.")
        }
        contents <-
          if (exitCode == 0)
            EitherT.right[String](readFile(target))
          else EitherT.leftT[IO, Array[Byte]]("That's incorrect Typst code.")
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
        "typst",
        "compile",
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
