package io.github.armeddon.bot

import cats.Parallel
import cats.effect._
import cats.data.EitherT

import telegramium.bots.high._
import telegramium.bots.high.implicits._

import org.typelevel.log4cats.Logger

import io.github.armeddon.format.{Format, Message => MsgFormat}
import io.github.armeddon.compile.TypstBuilder
import io.github.armeddon.upload._

class InlineTypstBot(apiUrl: String)(implicit
    bot: Api[IO],
    asyncF: Async[IO],
    parallel: Parallel[IO],
    logger: Logger[IO]
) extends LongPollBot[IO](bot) {
  import telegramium.bots._

  override def onMessage(msg: Message): IO[Unit] = for {
    _ <- logger.info(
      new StringBuilder("Received message: ")
        .append(
          msg.text
            .getOrElse("")
        )
        .append(
          msg.from
            .map(user => s" from ${user.firstName} (${user.id.toString})")
            .getOrElse("")
        )
        .toString
    )
    _ <- msg.text match {
      case Some(text) if text.startsWith("/") => {
        val (cmd, rest) = text.tail.span(!_.isWhitespace)
        parseCommand(
          msg.chat.id,
          cmd,
          rest.drop(1)
        )
      }
      case Some(text) =>
        parseText(msg.chat.id, text, Format.default)
      case None => asyncF.unit
    }
  } yield ()

  override def onInlineQuery(query: InlineQuery): IO[Unit] =
    for {
      _ <- logger.info(
        new StringBuilder("Received inline query: ")
          .append(query.query)
          .append(" from ")
          .append(query.from.firstName)
          .append(s" (${query.from.id.toString})")
          .toString
      )
      image <- InlineTypstBot.getImage(
        apiUrl,
        query.query,
        Format.default
      )
      _ <- answerInline(query, image)
    } yield ()

  private def parseCommand(id: Long, cmd: String, text: String): IO[Unit] =
    for {
      _ <- logger.info(s"Received command: /$cmd.")
      _ <- Command
        .parse(cmd)
        .map {
          case Command.Format(fmt) =>
            for {
              _ <- logger.info(
                s"Set result format to ${fmt.toString}."
              )
              _ <- parseText(id, text, fmt)
            } yield ()
          case Command.Info => showInfo(id)
        }
        .getOrElse(IO.unit)
    } yield ()

  private def showInfo(id: Long): IO[Unit] =
    sendMessage(
      chatId = ChatIntId(id),
      text = InlineTypstBot.infoText
    ).exec.void

  private def parseText(
      id: Long,
      text: String,
      resultFormat: Format
  ): IO[Unit] =
    resultFormat.message match {
      case MsgFormat.Image =>
        for {
          image <- InlineTypstBot.getImage(apiUrl, text, resultFormat)
          _ <- answerOnMessageImage(id, image)
        } yield ()
      case MsgFormat.Text =>
        for {
          text <- InlineTypstBot.getText(text, resultFormat)
          _ <- answerOnMessageText(id, text)
        } yield ()
    }

  private def answerOnMessageImage(
      id: Long,
      image: Either[String, Image]
  ): IO[Unit] =
    image match {
      case Right(Image(url, _, _)) =>
        sendPhoto(
          chatId = ChatIntId(id),
          photo = InputLinkFile(url)
        ).exec.void
      case Left(error) => answerOnMessageError(id, error)
    }

  private def answerOnMessageText(
      id: Long,
      text: Either[String, String]
  ): IO[Unit] =
    text match {
      case Right(txt) =>
        sendMessage(
          chatId = ChatIntId(id),
          text = txt
        ).exec.void
      case Left(error) => answerOnMessageError(id, error)
    }

  private def answerOnMessageError(id: Long, error: String): IO[Unit] =
    sendMessage(
      chatId = ChatIntId(id),
      text = error
    ).exec.void

  private def answerInline(query: InlineQuery, image: Option[Image]): IO[Unit] =
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
object InlineTypstBot {
  private def getImage(
      apiUrl: String,
      code: String,
      format: Format
  )(implicit logger: Logger[IO]): IO[Either[String, Image]] =
    (for {
      compiled <- EitherT(TypstBuilder.build(code, format))
      encoded <- EitherT.rightT[IO, String](Encoder.encode(compiled))
      json <- EitherT.right[String](ImageUploader.upload(apiUrl)(encoded))
      parseResult = ResponseParser.parseImage(json)
      _ <- EitherT.liftF(
        if (parseResult.isDefined) {
          logger.info("Image uploaded successfully.")
        } else {
          logger.error("Image upload failed.")
        }
      )
      image <- EitherT.fromEither[IO](
        parseResult.toRight("Internal error")
      )
    } yield image).value

  private def getText(
      code: String,
      format: Format
  )(implicit logger: Logger[IO]): IO[Either[String, String]] =
    (for {
      compiled <- EitherT(TypstBuilder.build(code, format))
      text <- EitherT.rightT[IO, String](new String(compiled, "UTF-8"))
    } yield text).value

  private val infoText = """
This is a telegram bot, that compiles Typst for you
You can use the commands /png and /html to specify the output format.
The default output is a PNG image.

This bot can be used in inline mode. You can send a message starting with @InlineTypstBot and containing Typst source code in any chat to send a compiled image there instead.

You can see this message again by using either /help or /info command.
"""
}
