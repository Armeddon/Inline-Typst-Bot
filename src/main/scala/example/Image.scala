package example

import cats.effect._
import cats.data.OptionT

final case class Image(url: String, width: Int, height: Int)
object Image {
  def get(apiUrl: String, code: String): IO[Option[Image]] =
    (for {
      compiled <- OptionT(TypstBuilder.build(code))
      json <- OptionT.liftF(ImageUploader.upload(apiUrl)(compiled))
      image <- OptionT.fromOption[IO](ResponseParser.parse(json))
    } yield image).value
}
