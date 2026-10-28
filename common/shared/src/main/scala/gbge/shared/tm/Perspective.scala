package gbge.shared.tm

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

sealed trait Perspective {
  val id: Int
}

object Perspective {
  val codec: JsonCodec[Perspective] = Schema.derived.jsonCodec

  def apply(number: Int): Perspective = {
    if (number == 0) SpectatorPerspective else PlayerPerspective(number)
  }
}

case object SpectatorPerspective extends Perspective {
  override val id: Int = 0
}
case class PlayerPerspective(playerId: Int) extends Perspective {
  override val id: Int = playerId
}
