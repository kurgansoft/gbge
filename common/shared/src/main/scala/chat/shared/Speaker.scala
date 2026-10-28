package chat.shared

import gbge.shared.GameRole
import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class Speaker(override val roleId: Int) extends GameRole

object Speaker {
  val jsonCodec: JsonCodec[Speaker] = Schema.derived.jsonCodec
}
