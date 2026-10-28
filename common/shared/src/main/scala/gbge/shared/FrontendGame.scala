package gbge.shared

import gbge.shared.actions.GameAction
import zio.blocks.schema.json.Json

trait FrontendGame[GA <: GameAction] extends Game {
  lazy val encode: Json
}
