package example

sealed trait Command
object Command {
  def parse(cmd: String): Option[Command] = cmd match {
    case "png"  => Some(CommandFormat(new ResultFormatPNG))
    case "html" => Some(CommandFormat(new ResultFormatHTML))
    case "help" => Some(CommandInfo)
    case "info" => Some(CommandInfo)
    case _      => None
  }
}
final case class CommandFormat(resultFormat: ResultFormat) extends Command
final object CommandInfo extends Command
