package example

import cats.Parallel
import cats.effect._
import cats.data.OptionT

import telegramium.bots.high._
import telegramium.bots.high.implicits._

class InlineTypstBot(apiUrl: String)(implicit
    bot: Api[IO],
    asyncF: Async[IO],
    parallel: Parallel[IO]
) extends LongPollBot[IO](bot) {
  import telegramium.bots._

  override def onMessage(msg: Message): IO[Unit] =
    msg.text match {
      case Some(text) if text.startsWith("/") => {
        val (cmd, rest) = text.tail.span(!_.isWhitespace)
        parseCommand(
          msg.chat.id,
          cmd,
          rest.drop(1)
        )
      }
      case Some(text) =>
        parseText(msg.chat.id, text, ResultFormat.default)
      case None => asyncF.unit
    }

  override def onInlineQuery(query: InlineQuery): IO[Unit] =
    for {
      image <- InlineTypstBot.getImage(
        apiUrl,
        query.query,
        ResultFormat.default
      )
      _ <- answerInline(query, image)
    } yield ()

  private def parseCommand(id: Long, cmd: String, text: String): IO[Unit] =
    IO.println(s"Command is $cmd and text is $text") >>
      Command
        .parse(cmd)
        .map {
          case CommandFormat(fmt) => parseText(id, text, fmt)
          case CommandInfo        => showInfo(id)
        }
        .getOrElse(IO(()))

  private def showInfo(id: Long): IO[Unit] =
    sendMessage(
      chatId = ChatIntId(id),
      text = InlineTypstBot.infoText
    ).exec.void

  private def parseText(
      id: Long,
      text: String,
      resultFormat: ResultFormat
  ): IO[Unit] =
    resultFormat.message match {
      case ResultMessageImage =>
        for {
          image <- InlineTypstBot.getImage(apiUrl, text, resultFormat)
          _ <- answerOnMessageImage(id, image)
        } yield ()
      case ResultMessageText =>
        for {
          text <- InlineTypstBot.getText(text, resultFormat)
          _ <- answerOnMessageText(id, text)
        } yield ()
    }

  private def answerOnMessageImage(id: Long, image: Option[Image]): IO[Unit] =
    image map { case Image(url, _, _) =>
      sendPhoto(
        chatId = ChatIntId(id),
        photo = InputLinkFile(url)
      ).exec.void
    } getOrElse answerOnMessageError(id)

  private def answerOnMessageText(id: Long, text: Option[String]): IO[Unit] =
    text map { txt =>
      sendMessage(
        chatId = ChatIntId(id),
        text = txt
      ).exec.void
    } getOrElse answerOnMessageError(id)

  private def answerOnMessageError(id: Long): IO[Unit] =
    sendMessage(
      chatId = ChatIntId(id),
      text = "That's incorrect Typst!"
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
      resultFormat: ResultFormat
  ): IO[Option[Image]] =
    (for {
      compiled <- OptionT(TypstBuilder.build(code, resultFormat))
      encoded <- OptionT.pure[IO](Encoder.encode(compiled))
      json <- OptionT.liftF(ImageUploader.upload(apiUrl)(encoded))
      image <- OptionT.fromOption[IO](ResponseParser.parseImage(json))
    } yield image).value

  private def getText(
      code: String,
      resultFormat: ResultFormat
  ): IO[Option[String]] =
    (for {
      compiled <- OptionT(TypstBuilder.build(code, resultFormat))
      text <- OptionT.pure[IO](new String(compiled, "UTF-8"))
    } yield text).value

  private val infoText = """
This is a telegram bot, that compiles Typst for you
You can use the commands /png and /html to specify the output format.
The default output value is a .png image.

This bot can be used in inline mode. You can send a message with @InlineTypstBot in any chat to send a compiled image there.

You can see this message again by using either /help or /info command.
"""
}
