package gbge.shared.tm

import zio.json._
import zio.schema.{DeriveSchema, Schema}

sealed trait TMMessage

object TMMessage {
  implicit val codec: JsonCodec[TMMessage] =
    DeriveJsonCodec.gen[TMMessage]

  implicit val schema: Schema[TMMessage] = DeriveSchema.gen
}

case class PortalId(id: Int) extends TMMessage

case object Update extends TMMessage

