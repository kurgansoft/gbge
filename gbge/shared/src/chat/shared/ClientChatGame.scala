package chat.shared

import gbge.shared.{DecodeCapable, FrontendGame}
import upickle.default.{macroRW, ReadWriter => RW}

case class ClientChatGame(override val messages: List[Message]) extends AbstractChatGame with FrontendGame[ChatAction] {
  override def serialize(): String = upickle.default.write(this)

  override def decodeAction(payload: String): ChatAction = upickle.default.read[ChatAction](payload)
}

object ClientChatGame extends DecodeCapable {
  implicit def rw: RW[ClientChatGame] = macroRW

  override def decode(encodedForm: String): ClientChatGame = {
    upickle.default.read[ClientChatGame](encodedForm)
  }

  override val name: String = "Chat"
}
