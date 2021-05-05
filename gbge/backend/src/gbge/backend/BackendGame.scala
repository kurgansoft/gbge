package gbge.backend

import gbge.shared.actions.GameAction
import gbge.shared._

case class Player(id: Int, name: String, token: String, isAdmin: Boolean = false, role: Option[Int] = None) {
  def toFrontendPlayer(): FrontendPlayer = FrontendPlayer(id, name, isAdmin, role)
}

trait BackendGame[+fg <: FrontendGame[GameAction]] extends Game {

  implicit def cnToTuple2(result: UniverseResult): (Any, UniverseResult) = (this, result)

  val state: GameState

  val noOfPlayers: Int

  def increaseRoomSize(): (BackendGame[fg], UniverseResult) = {
    (this, GeneralFailure("Increasing the room size is not supported in this game!"))
  }

  def decreaseRoomSize(): (BackendGame[fg], UniverseResult) = {
    (this, GeneralFailure("Decreasing the room size is not supported in this game!"))
  }

  def reduce(gameAction: GameAction, invoker: Option[Player]): (BackendGame[fg], UniverseResult)

  def toFrontendGame(role: Option[Int] = None): fg
  def idToRoleDescription(id: Int): Option[String] = {
    try {
      Some(roles(id).toString)
    } catch {
      case e: Exception => None
    }
  }

  def decodeAction(payload: String): GameAction = this.toFrontendGame().decodeAction(payload)
}