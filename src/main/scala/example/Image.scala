package example

import cats.effect._
import cats.implicits._

case class Image(url: String, width: Int, height: Int)
object Image {
  def get(apiUrl: String, code: String): IO[Option[Image]] = {
    for {
      compiled <- TypstBuilder.build(code)
      image <- compiled.map(ImageUploader.upload(apiUrl)(_)).getOrElse(IO(None))
    } yield image
  }
}
