package gbge.shared.tm

import zio.json.*
import zio.schema.{DeriveSchema, Schema}

case class PortalCoordinates(
                              portalId: Int,
                              actionNumber: Option[Int],
                              selectedPerspective: Option[Perspective])

object PortalCoordinates {

  implicit val codec: JsonCodec[PortalCoordinates] =
    DeriveJsonCodec.gen[PortalCoordinates]

  implicit val schema: Schema[PortalCoordinates] = DeriveSchema.gen

}