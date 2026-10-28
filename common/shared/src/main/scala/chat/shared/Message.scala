package chat.shared

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class Message(roleNumber: Int, message: String)

object Message {
  val jsonCodec: JsonCodec[Message] = Schema.derived.jsonCodec
}
