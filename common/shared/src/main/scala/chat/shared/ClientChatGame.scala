package chat.shared

import gbge.shared.FrontendGame
import zio.json.JsonCodec
import zio.schema.{DeriveSchema, Schema}

case class ClientChatGame(override val messages: List[Message]) extends AbstractChatGame with FrontendGame[ChatAction] {
  override lazy val encode: zio.json.ast.Json = ClientChatGame.codec.encoder.toJsonAST(this).getOrElse(???)
}

object ClientChatGame {
  implicit val schema: Schema[ClientChatGame] = DeriveSchema.gen[ClientChatGame] 
  implicit val codec: JsonCodec[ClientChatGame] = 
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}
