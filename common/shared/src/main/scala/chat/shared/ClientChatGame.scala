package chat.shared

import gbge.shared.FrontendGame
import zio.blocks.schema.Schema
import zio.blocks.schema.json.{Json, JsonCodec}

case class ClientChatGame(override val messages: List[Message])
    extends AbstractChatGame
    with FrontendGame[ChatAction] {
  override lazy val encode: Json =
    ClientChatGame.jsonCodec.encodeToJson(this)
}

object ClientChatGame {
  val jsonCodec: JsonCodec[ClientChatGame] = Schema.derived.jsonCodec
}
