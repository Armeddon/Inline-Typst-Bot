package example

sealed trait Command
object Command {
  def parse(cmd: String): Option[Command] = cmd match {
    case "png"  => Some(CommandFormat(ResultFormatPNG))
    case "html" => Some(CommandFormat(ResultFormatHTML))
    case "help" => Some(CommandInfo)
    case "info" => Some(CommandInfo)
    case "start" => Some(CommandInfo)
    case _      => None
  }
}
final case class CommandFormat(resultFormat: ResultFormat) extends Command
final object CommandInfo extends Command
