package gbge.shared.tm

import zio.json.*
import zio.schema.{DeriveSchema, Schema}

sealed trait TMMessage

object TMMessage {
  implicit val schema: Schema[TMMessage] = DeriveSchema.gen
  implicit val codec: JsonCodec[TMMessage] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}

case class PortalId(id: Int) extends TMMessage

case object Update extends TMMessage

