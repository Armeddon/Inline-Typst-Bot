package io.github.armeddon.bot

import io.github.armeddon.format.{Format => ResultFormat}

sealed trait Command
object Command {
  def parse(cmd: String): Option[Command] = cmd match {
    case "png"   => Some(Format(ResultFormat.PNG))
    case "html"  => Some(Format(ResultFormat.HTML))
    case "help"  => Some(Info)
    case "info"  => Some(Info)
    case "start" => Some(Info)
    case _       => None
  }
  final case class Format(format: ResultFormat) extends Command
  final object Info extends Command
}
