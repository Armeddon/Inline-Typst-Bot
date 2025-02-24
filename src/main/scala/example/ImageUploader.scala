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

class ImageUploader(apiUrl: String) {
  def upload: File => IO[Option[Image]] = (
    readBytes _
      andThen
    encodeBytes _
      andThen
    multipart _
      andThen
    request _
      andThen
    sendRequest _
      andThen
    parseResponse _
  )
  private def readBytes(file: File): Array[Byte] = {
    Files.readAllBytes(Paths.get(file.toURI))
  }
  private def encodeBytes(bytes: Array[Byte]): String = {
    Base64.getEncoder().encodeToString(bytes)
  }
  private def multipart(encoded: String): Multipart[IO] = Multipart(
      Vector(Part.formData("image", encoded))
  )
  private def request(multipart: Multipart[IO]): Request[IO] = Request(
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
