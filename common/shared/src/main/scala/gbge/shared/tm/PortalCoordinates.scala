package gbge.shared.tm

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class PortalCoordinates(
    portalId: Int,
    actionNumber: Option[Int],
    selectedPerspective: Option[Perspective]
)

object PortalCoordinates {
  val codec: JsonCodec[PortalCoordinates] = Schema.derived.jsonCodec
}
