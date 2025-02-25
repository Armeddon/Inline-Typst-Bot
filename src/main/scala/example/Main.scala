package example

import cats.effect._

import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.middleware.Logger

import telegramium.bots.high.Api
import telegramium.bots.high.BotApi

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = 
    BlazeClientBuilder[IO].resource
      .use { httpClient =>
        val http = Logger(logBody = false, logHeaders = false)(httpClient)
        args match {
          case List(botToken, apiKey) =>
            implicit val api: Api[IO] = createBotBackend(http, botToken)
            val inlineTypstBot = new InlineTypstBot(apiUrl(apiKey))
            inlineTypstBot.start().as(ExitCode.Success)
          case _ =>
            IO.raiseError(new RuntimeException("Usage: \n Application $botToken $apiKey"))
        }
      }

    private def createBotBackend(http: Client[IO], token: String) =
      BotApi(http, baseUrl = telegramUrl(token))

    private def telegramUrl(token: String): String = s"https://api.telegram.org/bot$token"

    private def apiUrl(apiKey: String): String = s"https://api.imgbb.com/1/upload?expiration=60&key=$apiKey"
}
