package gbge.shared.tm

import upickle.default.{macroRW, ReadWriter => RW}

abstract sealed class TMMessage

object TMMessage {
  implicit def rw: RW[TMMessage] = macroRW
}

case class PortalId(id: Int) extends TMMessage

object PortalId {
  implicit def rw: RW[PortalId] = macroRW
}

case object Update extends TMMessage {
  implicit def rw: RW[Update.type] = macroRW
}

