package gbge.shared

import zio.json.JsonCodec
import zio.schema.{DeriveSchema, Schema}

case class JoinResponse(id: Int, token: String)

object JoinResponse {
  implicit val schema: Schema[JoinResponse] = DeriveSchema.gen
  implicit val codec: JsonCodec[JoinResponse] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}
