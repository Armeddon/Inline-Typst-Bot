package example

import io.circe.Json

import cats.implicits._

object ResponseParser {
  def parseImage(json: Json): Option[Image] =
    for {
      obj <- json.asObject
      data <- obj("data") >>= (_.asObject)
      url <- data("url") >>= (_.asString)
      width <- data("width") >>= (_.asNumber) >>= (_.toInt)
      height <- data("height") >>= (_.asNumber) >>= (_.toInt)
    } yield Image(url, width, height)
}
