package gbge.shared.tm

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

sealed trait TMMessage

object TMMessage {
  val codec: JsonCodec[TMMessage] = Schema.derived.jsonCodec
}

case class PortalId(id: Int) extends TMMessage

case object Update extends TMMessage
