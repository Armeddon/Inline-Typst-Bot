package io.github.armeddon

import cats.effect._

import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.middleware.{Logger => HttpLogger}

import telegramium.bots.high.Api
import telegramium.bots.high.BotApi

import io.github.armeddon.bot.InlineTypstBot
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

object Main extends IOApp {
  def program(args: List[String])(implicit logger: Logger[IO]): IO[Unit] =
    for {
      botToken <- readEnvVar("BOT_TOKEN")
      _ <- IO.raiseUnless(botToken.isDefined)(
        new RuntimeException("Environment variable BOT_TOKEN must be set")
      )
      apiKey <- readEnvVar("API_KEY")
      _ <- IO.raiseUnless(apiKey.isDefined)(
        new RuntimeException("Environment variable API_KEY must be set")
      )
      _ <-
        BlazeClientBuilder[IO].resource
          .use { httpClient =>
            val http =
              HttpLogger(logBody = false, logHeaders = false)(httpClient)
            implicit val api: Api[IO] = createBotBackend(http, botToken.get)
            val inlineTypstBot = new InlineTypstBot(apiUrl(apiKey.get))
            inlineTypstBot.start().as(ExitCode.Success)
          }
    } yield ()

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    program(args).as(ExitCode.Success)
  }

  private def readEnvVar(name: String): IO[Option[String]] =
    IO(sys.env.get(name))

  private def createBotBackend(http: Client[IO], token: String) =
    BotApi(http, baseUrl = telegramUrl(token))

  private def telegramUrl(token: String): String =
    s"https://api.telegram.org/bot$token"

  private def apiUrl(apiKey: String): String =
    s"https://api.imgbb.com/1/upload?expiration=60&key=$apiKey"
}
