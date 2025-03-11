package example

import io.circe.Json

object ResponseParser {
  def parse(json: Json): Option[Image] =
    for {
      obj <- json.asObject
      data <- obj("data").flatMap(_.asObject)
      url <- data("url").flatMap(_.asString)
      width <- data("width").flatMap(_.asNumber).flatMap(_.toInt)
      height <- data("height").flatMap(_.asNumber).flatMap(_.toInt)
      image <- Some(Image(url, width, height))
    } yield image
}
