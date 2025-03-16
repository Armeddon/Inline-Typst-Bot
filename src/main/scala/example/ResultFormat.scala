package example

sealed trait ResultFormat {
  def format: String
  def suffix: String = "." + format
  def message: ResultMessage
}
object ResultFormat {
  def default: ResultFormat = ResultFormatPNG
}
final object ResultFormatPNG extends ResultFormat {
  def format = "png"
  def message = ResultMessageImage
}
final object ResultFormatHTML extends ResultFormat {
  def format = "html"
  def message = ResultMessageText
}
