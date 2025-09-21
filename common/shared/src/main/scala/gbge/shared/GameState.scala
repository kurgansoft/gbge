package gbge.shared

import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

sealed trait GameState

object GameState {
  implicit val codec: JsonCodec[GameState] =
    DeriveJsonCodec.gen[GameState]

  implicit val schema: Schema[GameState] = DeriveSchema.gen

  case object NOT_STARTED extends GameState
  case object IN_PROGRESS extends GameState
}


