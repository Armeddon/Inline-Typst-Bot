package example

import cats.effect._
import cats.data.OptionT

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Base64

object TypstBuilder {
  def build(content: String): IO[Option[String]] =
    (for {
      sourceFile <- createTempFile(sourcePrefix, sourceSuffix)
      targetFile <- createTempFile(targetPrefix, targetSuffix)
    } yield (sourceFile, targetFile)).use { case (source, target) =>
      for {
        _ <- writeTypstCode(source, content)
        encoded <- compile(source, target)
      } yield encoded
    }

  private def createTempFile(
      prefix: String,
      suffix: String
  ): Resource[IO, File] =
    Resource.make(createFile(prefix, suffix))(file =>
      IO.blocking(file.delete).void
    )

  private def createFile(prefix: String, suffix: String): IO[File] =
    IO.blocking {
      val file = File.createTempFile(prefix, suffix)
      file.deleteOnExit()
      file
    }

  private def writeTypstCode(file: File, content: String): IO[Unit] =
    write(file, preamble ++ content)

  private def write(file: File, content: String): IO[Unit] =
    IO.blocking {
      Files.writeString(file.toPath, content, StandardOpenOption.WRITE)
    }.void

  private def compile(source: File, target: File): IO[Option[String]] =
    startTypstProcess(source, target).use { process =>
      (for {
        exitCode <- OptionT.liftF(IO.blocking(process.waitFor()))
        contents <-
          if (exitCode == 0)
            OptionT.liftF(readFile(target))
          else
            OptionT.none[IO, Array[Byte]]
      } yield encode(contents)).value
    }

  private def startTypstProcess(
      source: File,
      target: File
  ): Resource[IO, Process] =
    Resource.make {
      IO.blocking {
        new ProcessBuilder(
          "typst",
          "compile",
          source.getAbsolutePath(),
          target.getAbsolutePath(),
          "--format=png"
        ).redirectErrorStream(true)
          .start()
      }
    } { process =>
      IO.blocking {
        if (process.isAlive) process.destroy()
      }.void
    }

  private def readFile(file: File): IO[Array[Byte]] =
    IO.blocking(Files.readAllBytes(Paths.get(file.toURI)))
  private def encode(bytes: Array[Byte]): String =
    Base64.getEncoder().encodeToString(bytes)

  private lazy val sourcePrefix = "inline_typst_bot_source"
  private lazy val sourceSuffix = ".typ"
  private lazy val targetPrefix = "inline_typst_bot_target"
  private lazy val targetSuffix = ".png"

  private lazy val preamble =
    "#set page(height: auto, width: auto, margin: 5pt)\n"
}
