package gbge.shared.tm

import gbge.shared.FrontendUniverse
import zio.json._
import zio.schema.{DeriveSchema, Schema}

sealed trait PortalMessage

object PortalMessage {
  implicit val codec: JsonCodec[PortalMessage] =
    DeriveJsonCodec.gen[PortalMessage]

  implicit val schema: Schema[PortalMessage] = DeriveSchema.gen
}

case class PortalMessageWithPayload(rawFU: String , perspective: Perspective) extends PortalMessage

object PortalMessageWithPayload {

  def create(fu: FrontendUniverse, perspective: Perspective): PortalMessageWithPayload = {
    PortalMessageWithPayload(fu.toString(), perspective)
  }
}

case object ActionNeedsToBeSelected extends PortalMessage

case class PerspectiveNeedsToBeSelected(selectedAction: Int) extends PortalMessage

object PerspectiveNeedsToBeSelected


case object MysteriousError0 extends PortalMessage
