package gbge.shared.tm

import zio.json._
import zio.schema.{DeriveSchema, Schema}

sealed trait Perspective {
  val id: Int
}

object Perspective {
  implicit val schema: Schema[Perspective] = DeriveSchema.gen
  implicit val codec: JsonCodec[Perspective] =
    zio.schema.codec.JsonCodec.jsonCodec(schema)

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