package chat.shared

import gbge.shared.FrontendGame
import zio.blocks.schema.Schema
import zio.blocks.schema.json.{Json, JsonCodec}

case class ClientChatGame(
    override val messages: List[Message] = List.empty,
    override val activeRoles: Set[Speaker] = Set.empty
) extends AbstractChatGame(messages, activeRoles)
    with FrontendGame[Speaker, ChatAction] {
  override lazy val encode: Json =
    ClientChatGame.jsonCodec.encodeToJson(this)
}

object ClientChatGame {
  val jsonCodec: JsonCodec[ClientChatGame] = Schema.derived.jsonCodec
}
