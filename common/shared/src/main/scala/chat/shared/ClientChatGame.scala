package chat.shared

import gbge.shared.FrontendGame
import zio.json.{DeriveJsonCodec, JsonCodec}

case class ClientChatGame(override val messages: List[Message]) extends AbstractChatGame with FrontendGame[ChatAction] {
  override lazy val encode: zio.json.ast.Json = ClientChatGame.codec.encoder.toJsonAST(this).getOrElse(???)
}

object ClientChatGame {
  implicit val codec: JsonCodec[ClientChatGame] = DeriveJsonCodec.gen[ClientChatGame]
}
