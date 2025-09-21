package gbge.shared

import gbge.shared.actions.GameAction

trait FrontendGame[GA <: GameAction] extends Game {
  lazy val encode: zio.json.ast.Json
}
