package gbge.backend

import gbge.shared._
import gbge.shared.actions.{Action, GameAction}

trait Startable {
  def start(noOfPlayers: Int): (BackendGame[_ <: FrontendGame[_ <: GameAction]], Option[Action])

  val frontendGame: DecodeCapable

  val name: String
}
