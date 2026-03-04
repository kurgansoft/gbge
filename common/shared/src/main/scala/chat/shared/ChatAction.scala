package chat.shared

import gbge.shared.actions.GameAction
import zio.json.{EncoderOps, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

sealed trait ChatAction extends GameAction {
  override def convertToJson(): String =
    this.toJson
}

object ChatAction {
  implicit val schema: Schema[ChatAction] = DeriveSchema.gen
  implicit val codec: JsonCodec[ChatAction] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}

case class SendMessage(message: String) extends ChatAction