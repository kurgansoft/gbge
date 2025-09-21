package chat.shared

import gbge.shared.actions.GameAction
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

sealed trait ChatAction extends GameAction

object ChatAction {
  implicit val schema: Schema[ChatAction] = DeriveSchema.gen

  implicit val codec: JsonCodec[ChatAction] =
    DeriveJsonCodec.gen[ChatAction]
}

case class SendMessage(message: String) extends ChatAction