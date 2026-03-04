package gbge.shared.tm

import gbge.shared.actions.*
import zio.json.JsonCodec
import zio.schema.{DeriveSchema, Schema}

case class ActionStackInTransit(entries: List[EncodedActionInvokerAndPlayers]) {
  def toActionEntries()(implicit actionCodecs: List[JsonCodec[_ <: GameAction]]): List[ActionInvokerAndPlayers] = entries.map(_.convertToActionAndInvoker())
}

object ActionStackInTransit {
  implicit val schema: Schema[ActionStackInTransit] = DeriveSchema.gen
  implicit val jsonCodec: JsonCodec[ActionStackInTransit] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}


