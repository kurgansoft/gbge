package chat.shared

import gbge.shared.actions.GameAction
import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

sealed trait ChatAction extends GameAction {
  override lazy val asJsonString: String =
    ChatAction.jsonCodec.encodeToString(this)
}

object ChatAction {
  val jsonCodec: JsonCodec[ChatAction] = Schema.derived.jsonCodec
}

case class SendMessage(message: String) extends ChatAction
