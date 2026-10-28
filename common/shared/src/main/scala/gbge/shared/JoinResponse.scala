package gbge.shared

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class JoinResponse(id: Int, token: String)

object JoinResponse {
  val codec: JsonCodec[JoinResponse] = Schema.derived.jsonCodec
}
