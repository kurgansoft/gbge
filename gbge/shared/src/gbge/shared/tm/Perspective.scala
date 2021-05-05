package gbge.shared.tm

import upickle.default.{macroRW, ReadWriter => RW}

abstract sealed class Perspective {
  val id: Int
}

case object SpectatorPerspective extends Perspective {
  override val id: Int = 0
}
case class PlayerPerspective(playerId: Int) extends Perspective {
  override val id: Int = playerId
}

object Perspective {
  implicit def rw: RW[Perspective] = macroRW

  def apply(number: Int): Perspective = {
    if (number == 0) SpectatorPerspective else PlayerPerspective(number)
  }
}

object PlayerPerspective {
  implicit def rw: RW[PlayerPerspective] = macroRW
}