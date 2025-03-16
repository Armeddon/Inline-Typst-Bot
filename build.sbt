import Dependencies._

ThisBuild / scalaVersion     := "2.13.15"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.armeddon"
ThisBuild / organizationName := "armeddon"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision;
ThisBuild / scalacOptions += {
    "-Wunused:imports"
}

lazy val root = (project in file("."))
  .settings(
    name := "InlineTypstBot",
    libraryDependencies ++= Seq(
      munit % Test,
      telegramium_core,
      telegramium_high,
      cats_effect,
    )
  )

wartremoverErrors ++= Warts.allBut(Wart.Any, Wart.Nothing, Wart.OptionPartial, Wart.ImplicitParameter)
wartremoverWarnings ++= Warts.allBut(Wart.Any, Wart.Nothing, Wart.OptionPartial, Wart.ImplicitParameter)
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
