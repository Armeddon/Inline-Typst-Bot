package example

import cats.effect._

import cats.implicits._

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.io.File

import java.util.Base64

import org.http4s.client._
import org.http4s.client.blaze._
import org.http4s._
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.multipart._

import io.circe.Json
import io.circe.syntax._

case class Image(url: String, width: Int, height: Int)

object TypstBuilder {
  def build(content: String, imgbbKey: String): IO[Option[Image]] =
    (for {
      sourceFile <- createTempFile(sourcePrefix, sourceSuffix)
      targetFile <- createTempFile(targetPrefix, targetSuffix)
    } yield (sourceFile, targetFile)).use { case (source, target) =>
      for {
        _ <- write(source, "#set page(height: auto, width: auto, margin: 5pt)\n" ++ content)
        success <- compile(source, target)
        image <- if (success) upload(target, imgbbKey) else IO(None)
        _ <- IO.consoleForIO.println(image.toString)
      } yield image
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

  private def upload(file: File, imgbbKey: String): IO[Option[Image]] = {
    val bytes = Files.readAllBytes(Paths.get(file.toURI))
    val base64 = Base64.getEncoder().encodeToString(bytes)

    val url = s"https://api.imgbb.com/1/upload?expiration=60&key=$imgbbKey"

    val multipart = Multipart[IO](
      Vector(Part.formData("image", base64))
    )
    val request = Request[IO](
      method = POST,
      uri = Uri.unsafeFromString(url)
    ).withEntity(multipart)
     .putHeaders(multipart.headers)

    BlazeClientBuilder[IO].resource.use { client =>
      client.expect[Json](request).map { response =>
        Some(Image(
          response.asObject.get.apply("data").get.asObject.get.apply("url").get.asString.get,
          response.asObject.get.apply("data").get.asObject.get.apply("width").get.asNumber.get.toInt.get,
          response.asObject.get.apply("data").get.asObject.get.apply("height").get.asNumber.get.toInt.get
        ))
      }
    }
  }

   private lazy val sourcePrefix = "inline_typst_bot_source"
   private lazy val sourceSuffix = ".typ"
   private lazy val targetPrefix = "inline_typst_bot_target"
   private lazy val targetSuffix = ".png"
}
