package example

import cats.effect._

import cats.implicits._

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.io.File

import java.util.Base64

object TypstBuilder {
  def build(content: String): IO[Option[String]] =
    (for {
      sourceFile <- createTempFile(sourcePrefix, sourceSuffix)
      targetFile <- createTempFile(targetPrefix, targetSuffix)
    } yield (sourceFile, targetFile)).use { case (source, target) =>
      for {
        _ <- write(source, "#set page(height: auto, width: auto, margin: 5pt)\n" ++ content)
        success <- compile(source, target)
        bytes <- if (success) IO(Some(Files.readAllBytes(Paths.get(target.toURI)))) else IO(None)
        encoded <- if (success) IO(Some(Base64.getEncoder().encodeToString(bytes.get))) else IO(None)
      } yield encoded
    }

  private def createTempFile(prefix: String, suffix: String): Resource[IO, File] =
    Resource.make(createFile(prefix, suffix))(file => IO.blocking(file.delete).void)

  private def createFile(prefix: String, suffix: String): IO[File] =
    IO.blocking {
      val file = File.createTempFile(prefix, suffix)
      file.deleteOnExit()
      file
    }

  private def write(file: File, content: String): IO[Unit] =
    IO.blocking {
      Files.writeString(file.toPath, content, StandardOpenOption.WRITE)
    }.void

  private def compile(source: File, target: File): IO[Boolean] =
    Resource.make {
      IO.blocking {
        new ProcessBuilder("typst", "compile", source.getAbsolutePath(), target.getAbsolutePath(), "--format=png")
          .redirectErrorStream(true)
          .start()
      }
    } { process =>
      IO.blocking {
        if (process.isAlive) process.destroy()
      }.void
    }.use { process =>
      IO.blocking(process.waitFor()).map {
        case 0 => true
        case _ => false
      }
    }

   private lazy val sourcePrefix = "inline_typst_bot_source"
   private lazy val sourceSuffix = ".typ"
   private lazy val targetPrefix = "inline_typst_bot_target"
   private lazy val targetSuffix = ".png"
}
