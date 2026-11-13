package chat.shared

import gbge.shared.GameRole
import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class Speaker(override val roleId: Int) extends GameRole

object Speaker {
  val jsonCodec: JsonCodec[Speaker] = Schema.derived.jsonCodec
  val all: Set[Speaker] =
    Set(Speaker(1), Speaker(2), Speaker(3), Speaker(4), Speaker(5), Speaker(6))
}
