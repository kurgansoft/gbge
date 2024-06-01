package chat.shared

import gbge.shared.GameRole
import upickle.default.{macroRW, ReadWriter => RW}

case class Speaker(override val roleId: Int) extends GameRole

object Speaker {
  implicit def rw: RW[Speaker] = macroRW
}
