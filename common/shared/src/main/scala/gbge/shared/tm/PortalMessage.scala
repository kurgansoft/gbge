package gbge.shared.tm

import gbge.shared.FrontendUniverse
import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

sealed trait PortalMessage

object PortalMessage {
  val codec: JsonCodec[PortalMessage] = Schema.derived.jsonCodec
}

case class PortalMessageWithPayload(rawFU: String, perspective: Perspective)
    extends PortalMessage

object PortalMessageWithPayload {

  def create(
      fu: FrontendUniverse,
      perspective: Perspective
  ): PortalMessageWithPayload = {
    PortalMessageWithPayload(fu.toString(), perspective)
  }
}

case object ActionNeedsToBeSelected extends PortalMessage

case class PerspectiveNeedsToBeSelected(selectedAction: Int)
    extends PortalMessage

object PerspectiveNeedsToBeSelected

case object MysteriousError0 extends PortalMessage
