package gbge.shared

import gbge.shared.actions.GameAction
import zio.blocks.schema.json.Json

trait FrontendGame[GR <: GameRole, GA <: GameAction] extends Game[GR, GA] {
  lazy val encode: Json
}
