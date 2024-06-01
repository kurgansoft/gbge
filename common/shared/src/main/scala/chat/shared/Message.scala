package chat.shared

import upickle.default.{macroRW, ReadWriter => RW}

case class Message(roleNumber: Int, message: String)

object Message {
  implicit def rw: RW[Message] = macroRW
}
