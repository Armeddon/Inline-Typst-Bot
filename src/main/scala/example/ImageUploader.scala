package example

import cats.effect._

import cats.implicits._

import java.nio.file.Files
import java.nio.file.Paths
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
import org.http4s.blaze.client.BlazeClient

object ImageUploader {
  def upload(implicit apiUrl: String): String => IO[Option[Image]] = (
    multipart _
      andThen
    request _
      andThen
    sendRequest _
      andThen
    parseResponse _
  )
  private def multipart(encoded: String): Multipart[IO] = Multipart(
      Vector(Part.formData("image", encoded))
  )
  private def request(multipart: Multipart[IO])(implicit apiUrl: String): Request[IO] = Request(
    method = POST,
    uri = Uri.unsafeFromString(apiUrl)
  ).withEntity(multipart).putHeaders(multipart.headers)
  private def sendRequest(request: Request[IO]): IO[Json] =
    BlazeClientBuilder[IO].resource.use { client =>
      client.expect[Json](request)
    }
  private def parseResponse(json: IO[Json]): IO[Option[Image]] =
    json.map { json =>
      Some(Image(
        json.asObject.get.apply("data").get.asObject.get.apply("url").get.asString.get,
        json.asObject.get.apply("data").get.asObject.get.apply("width").get.asNumber.get.toInt.get,
        json.asObject.get.apply("data").get.asObject.get.apply("height").get.asNumber.get.toInt.get
      ))
  }
}
