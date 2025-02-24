package example

import cats.implicits._

import cats.effect._
import cats.effect.Async
import cats.Parallel

import telegramium.bots.high._
import telegramium.bots.high.implicits._
import telegramium.bots.Message

import java.nio.file.{Files, Paths}
import java.util.Base64

class InlineTypstBot(apiKey: String)(implicit
  bot: Api[IO],
  asyncF: Async[IO],
  parallel: Parallel[IO]
  ) extends LongPollBot[IO](bot) {

    import cats.syntax.flatMap._
    import cats.syntax.functor._
    import telegramium.bots._

    override def onMessage(msg: Message): IO[Unit] = {
      msg.text match {
        case Some(s) if s.toLowerCase.startsWith("/") =>
          sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = "That's some command",
          ).exec.void
        case Some(_) => 
          sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = "Hello, world!",
          ).exec.void
        case None => asyncF.unit
      }
    }

    override def onInlineQuery(query: InlineQuery): IO[Unit] = {
      def answer(image: Option[Image]): IO[Unit] = {
        answerInlineQuery(
          inlineQueryId = query.id,
          results = image.map {
            case Image(url, width, height) => InlineQueryResultPhoto(
              id = "0",
              photoUrl = url,
              thumbnailUrl = url,
              photoWidth = Some(width),
              photoHeight = Some(height))
            }.toList,
          cacheTime = Some(0),
        ).exec.void
      }

      for {
        result <- TypstBuilder.build(query.query, apiKey)
        _ <- answer(result)
      } yield ()
    }
}
