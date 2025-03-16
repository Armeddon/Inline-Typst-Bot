package io.github.armeddon.upload

import java.util.Base64

object Encoder {
  def encode(bytes: Array[Byte]): String =
    Base64.getEncoder().encodeToString(bytes)
}
