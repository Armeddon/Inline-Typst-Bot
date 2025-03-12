package example

import cats.Parallel
import cats.effect._
import cats.data.OptionT

import telegramium.bots.high._
import telegramium.bots.high.implicits._

import java.util.Locale

class InlineTypstBot(apiUrl: String)(implicit
    bot: Api[IO],
    asyncF: Async[IO],
    parallel: Parallel[IO]
) extends LongPollBot[IO](bot) {
  import telegramium.bots._

  override def onMessage(msg: Message): IO[Unit] = {
    msg.text match {
      case Some(s) if s.toLowerCase(Locale.US).startsWith("/") =>
        sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = "That's some command"
        ).exec.void
      case Some(_) =>
        sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = "Hello, world!"
        ).exec.void
      case None => asyncF.unit
    }
  }

  override def onInlineQuery(query: InlineQuery): IO[Unit] = {
    for {
      image <- InlineTypstBot.get(apiUrl, query.query)
      _ <- answer(query, image)
    } yield ()
  }

  private def answer(query: InlineQuery, image: Option[Image]): IO[Unit] = {
    answerInlineQuery(
      inlineQueryId = query.id,
      results = image.map { case Image(url, width, height) =>
        InlineQueryResultPhoto(
          id = "0",
          photoUrl = url,
          thumbnailUrl = url,
          photoWidth = Some(width),
          photoHeight = Some(height)
        )
      }.toList,
      cacheTime = Some(0)
    ).exec.void
  }
}
object InlineTypstBot {
  private def get(apiUrl: String, code: String): IO[Option[Image]] =
    (for {
      compiled <- OptionT(TypstBuilder.build(code))
      encoded <- OptionT.pure[IO](Encoder.encode(compiled))
      json <- OptionT.liftF(ImageUploader.upload(apiUrl)(encoded))
      image <- OptionT.fromOption[IO](ResponseParser.parse(json))
    } yield image).value
}
