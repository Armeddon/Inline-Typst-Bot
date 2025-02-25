package example

import cats.effect._
import io.circe.Json
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze._
import org.http4s.multipart._

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
  private def request(
      multipart: Multipart[IO]
  )(implicit apiUrl: String): Request[IO] = Request(
    method = POST,
    uri = Uri.unsafeFromString(apiUrl)
  ).withEntity(multipart).putHeaders(multipart.headers)
  private def sendRequest(request: Request[IO]): IO[Json] =
    BlazeClientBuilder[IO].resource.use { client =>
      client.expect[Json](request)
    }
  private def parseResponse(json: IO[Json]): IO[Option[Image]] =
    json.map { json =>
      Some(
        Image(
          json.asObject.get
            .apply("data")
            .get
            .asObject
            .get
            .apply("url")
            .get
            .asString
            .get,
          json.asObject.get
            .apply("data")
            .get
            .asObject
            .get
            .apply("width")
            .get
            .asNumber
            .get
            .toInt
            .get,
          json.asObject.get
            .apply("data")
            .get
            .asObject
            .get
            .apply("height")
            .get
            .asNumber
            .get
            .toInt
            .get
        )
      )
    }
}
