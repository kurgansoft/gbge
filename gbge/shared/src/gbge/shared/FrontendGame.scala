package gbge.shared

import gbge.shared.actions.GameAction
import upickle.default.{macroRW, ReadWriter => RW}

trait Game {
  val minPlayerNumber: Int
  val maxPlayerNumber: Int

  val roles: List[GameRole]

  def getRoleById(id: Int) : Option[GameRole] = roles.find(_.roleId == id)

}

abstract sealed class GameState

object GameState {
  implicit def rw: RW[GameState] = macroRW
}

case object NOT_STARTED extends GameState
case object IN_PROGRESS extends GameState

trait FrontendGame[+ga <: GameAction] extends Game {
  def serialize(): String

  def decodeAction(payload: String): ga
}
