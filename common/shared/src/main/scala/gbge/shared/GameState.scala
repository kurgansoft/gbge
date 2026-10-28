package gbge.shared

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

sealed trait GameState

object GameState {
  val codec: JsonCodec[GameState] = Schema.derived.jsonCodec

  case object NOT_STARTED extends GameState
  case object IN_PROGRESS extends GameState
}
