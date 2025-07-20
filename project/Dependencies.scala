import sbt._

object Dependencies {
  private lazy val munit = "org.scalameta" %% "munit" % "0.7.29"

  private lazy val `telegramium-core` = "io.github.apimorphism" %% "telegramium-core" % "9.802.0"
  private lazy val `telegramium-high` = "io.github.apimorphism" %% "telegramium-high" % "9.802.0"

  private lazy val `cats-effect` = "org.typelevel" %% "cats-effect" % "3.5.7"

  private lazy val `log4cats-core` = "org.typelevel" %% "log4cats-core" % "2.6.0"
  private lazy val `log4cats-slf4j` = "org.typelevel" %% "log4cats-slf4j" % "2.6.0"
  private lazy val logback = "ch.qos.logback" % "logback-classic" % "1.4.14"

  lazy val `core-dependencies` = Seq(
    `telegramium-core`,
    `telegramium-high`,
    `cats-effect`,
    `log4cats-core`,
    `log4cats-slf4j`,
    logback
  )

  lazy val `test-dependencies` = Seq(munit % Test)
}
