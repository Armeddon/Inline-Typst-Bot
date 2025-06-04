package io.github.armeddon

import cats.effect._

import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.middleware.Logger

import telegramium.bots.high.Api
import telegramium.bots.high.BotApi

import io.github.armeddon.bot.InlineTypstBot

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      botToken <- readEnvVar("BOT_TOKEN")
      apiKey <- readEnvVar("API_KEY")
      exitCode <- if (botToken.isDefined && apiKey.isDefined)
        BlazeClientBuilder[IO].resource
          .use { httpClient =>
            val http = Logger(logBody = false, logHeaders = false)(httpClient)
            implicit val api: Api[IO] = createBotBackend(http, botToken.get)
            val inlineTypstBot = new InlineTypstBot(apiUrl(apiKey.get))
            inlineTypstBot.start().as(ExitCode.Success)
          }
        else 
          IO.raiseError(
            new RuntimeException("Environment variables BOT_TOKEN and API_KEY must be set")
          ) >> IO(ExitCode.Success)
    } yield exitCode

  private def readEnvVar(name: String): IO[Option[String]] =
    IO(sys.env.get(name))

  private def createBotBackend(http: Client[IO], token: String) =
    BotApi(http, baseUrl = telegramUrl(token))

  private def telegramUrl(token: String): String =
    s"https://api.telegram.org/bot$token"

  private def apiUrl(apiKey: String): String =
    s"https://api.imgbb.com/1/upload?expiration=60&key=$apiKey"
}
