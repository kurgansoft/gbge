package gbge.shared.tm

import gbge.shared.FrontendUniverse
import upickle.default.{macroRW, ReadWriter => RW}

abstract sealed class PortalMessage()

object PortalMessage {
  implicit def rw: RW[PortalMessage] = macroRW
}

case class PortalMessageWithPayload(rawFU: String , perspective: Perspective) extends PortalMessage

object PortalMessageWithPayload {
  implicit def rw: RW[PortalMessageWithPayload] = macroRW

  def create(fu: FrontendUniverse, perspective: Perspective): PortalMessageWithPayload = {
    PortalMessageWithPayload(fu.serialize(), perspective)
  }
}

case object ActionNeedsToBeSelected extends PortalMessage

case class PerspectiveNeedsToBeSelected(selectedAction: Int) extends PortalMessage

object PerspectiveNeedsToBeSelected {
  implicit def rw: RW[PerspectiveNeedsToBeSelected] = macroRW
}

case object MysteriousError0 extends PortalMessage
