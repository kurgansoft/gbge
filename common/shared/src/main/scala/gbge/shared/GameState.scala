package gbge.shared

import zio.json.JsonCodec
import zio.schema.{DeriveSchema, Schema}

sealed trait GameState

object GameState {
  implicit val schema: Schema[GameState] = DeriveSchema.gen
  implicit val codec: JsonCodec[GameState] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)

  case object NOT_STARTED extends GameState
  case object IN_PROGRESS extends GameState
}


