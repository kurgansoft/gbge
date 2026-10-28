package gbge.shared.tm

import gbge.shared.actions.*
import zio.blocks.schema.Schema
import zio.blocks.schema.json.{Json, JsonCodec}

case class ActionStackInTransit(entries: List[EncodedActionInvokerAndPlayers]) {
  def toActionEntries()(implicit
      actionCodecs: List[JsonCodec[? <: GameAction]]
  ): List[ActionInvokerAndPlayers] = entries.map(_.convertToActionAndInvoker())

  lazy val encode: Json = ActionStackInTransit.jsonCodec.encodeToJson(this)
}

object ActionStackInTransit {
  val jsonCodec: JsonCodec[ActionStackInTransit] = Schema.derived.jsonCodec

  def decode(json: Json): ActionStackInTransit =
    jsonCodec
      .decode(json)
      .getOrElse(
        throw new RuntimeException("could not decode ActionStackInTransit")
      )
}
