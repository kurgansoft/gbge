package gbge.backend

import gbge.shared.actions.{Action, GameAction}
import gbge.shared.*

trait BackendGameProps[GA <: GameAction, FG <: FrontendGame[GA]]
    extends GameProps[GA, FG] {
  def start(noOfPlayers: Int): (BackendGame[? <: GA, ? <: FG], Option[Action])
}
