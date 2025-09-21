package chat.shared

import zio.json.{DeriveJsonCodec, JsonCodec}


case class Message(roleNumber: Int, message: String)

object Message {
  implicit val codec: JsonCodec[Message] = DeriveJsonCodec.gen[Message]
}
