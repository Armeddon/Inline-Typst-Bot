package io.github.armeddon.format

sealed trait Message
object Message {
  final object Image extends Message
  final object Text extends Message
}
