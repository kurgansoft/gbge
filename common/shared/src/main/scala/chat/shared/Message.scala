package chat.shared

import zio.json.JsonCodec
import zio.schema.{DeriveSchema, Schema}

case class Message(roleNumber: Int, message: String)

object Message {
  implicit val schema: Schema[Message] = DeriveSchema.gen
  implicit val codec: JsonCodec[Message] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)

}
