package io.github.armeddon.format

sealed trait Format {
  def format: String
  def suffix: String = "." + format
  def message: Message
}
object Format {
  def default: Format = PNG
  final object PNG extends Format {
    def format = "png"
    def message = Message.Image
  }
  final object HTML extends Format {
    def format = "html"
    def message = Message.Text
  }
}
