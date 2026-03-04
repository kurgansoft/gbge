package gbge.shared.tm

import zio.json.*
import zio.schema.{DeriveSchema, Schema}

case class PortalCoordinates(
                              portalId: Int,
                              actionNumber: Option[Int],
                              selectedPerspective: Option[Perspective])

object PortalCoordinates {
  implicit val schema: Schema[PortalCoordinates] = DeriveSchema.gen
  implicit val codec: JsonCodec[PortalCoordinates] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)
}