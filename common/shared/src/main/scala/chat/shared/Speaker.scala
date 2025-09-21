package chat.shared

import gbge.shared.GameRole
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

case class Speaker(override val roleId: Int) extends GameRole

object Speaker {

  implicit val schema: Schema[Speaker] = DeriveSchema.gen
  
  implicit val codec: JsonCodec[Speaker] =
    DeriveJsonCodec.gen[Speaker]
}
