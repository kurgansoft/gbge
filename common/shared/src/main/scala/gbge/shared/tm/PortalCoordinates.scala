package gbge.shared.tm

import upickle.default.{macroRW, ReadWriter => RW}

case class PortalCoordinates(
                              portalId: Int,
                              actionNumber: Option[Int],
                              selectedPerspective: Option[Perspective])

object PortalCoordinates {
  implicit def rw: RW[PortalCoordinates] = macroRW
}