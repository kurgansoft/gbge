package gbge.backend

import gbge.shared.actions.{Action, GameAction}
import gbge.shared.*

trait BackendGameProps[GR <: GameRole, GA <: GameAction, FG <: FrontendGame[
  GR,
  GA
]] extends GameProps[GR, GA, FG] {
  def start(
      noOfPlayers: Int
  ): (BackendGame[? <: GR, ? <: GA, ? <: FG], Option[Action])
}
