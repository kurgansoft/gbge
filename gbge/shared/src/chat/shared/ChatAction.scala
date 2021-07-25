package chat.shared

import gbge.shared.actions.GameAction
import upickle.default.{macroRW, ReadWriter => RW}

abstract sealed class ChatAction extends GameAction {
  override def serialize(): String = upickle.default.write[ChatAction](this)
}

object ChatAction {
  implicit def rw: RW[ChatAction] = macroRW
}

case class SendMessage(message: String) extends ChatAction

object SendMessage {
  implicit def rw: RW[SendMessage] = macroRW
}