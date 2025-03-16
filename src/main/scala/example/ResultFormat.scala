package example

sealed trait ResultFormat {
  def format: String
  def suffix: String = "." + format
  def message: ResultMessage
}
object ResultFormat {
  def default: ResultFormat = new ResultFormatPNG
}
final class ResultFormatPNG extends ResultFormat {
  def format = "png"
  def message = ResultMessageImage
}
final class ResultFormatHTML extends ResultFormat {
  def format = "html"
  def message = ResultMessageText
}
