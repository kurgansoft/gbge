package gbge.shared

import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

case class JoinResponse(id: Int, token: String)

object JoinResponse {
  implicit val codec: JsonCodec[JoinResponse] =
    DeriveJsonCodec.gen[JoinResponse]

  implicit val schema: Schema[JoinResponse] = DeriveSchema.gen
}
