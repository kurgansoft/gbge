package gbge.backend

import gbge.backend.models.Player
import gbge.shared.*
import gbge.shared.actions.{Action, GameAction}
import zio.IO

import scala.language.implicitConversions

trait BackendGame[GA <: GameAction, FG <: FrontendGame[GA]] extends Game {

  implicit def cnToTuple2(result: UniverseResult): (Any, UniverseResult) = (this, result)

  val state: GameState

  val noOfPlayers: Int

  def increaseRoomSize(): Either[Failure, BackendGame[_ <: GA, _ <: FG]] =
    Left(GeneralFailure("Increasing the room size is not supported in this game!"))

  def decreaseRoomSize(): Either[Failure, BackendGame[_ <: GA, _ <: FG]] =
    Left(GeneralFailure("Decreasing the room size is not supported in this game!"))

  def reduce(gameAction: GameAction, invoker: Player): Either[Failure, (BackendGame[_ <: GA, _ <: FG], IO[Failure, Seq[Action]])]

  def toFrontendGame(role: Option[Int] = None): FG

  def idToRoleDescription(id: Int): Option[String] = {
    try {
      Some(roles(id).toString)
    } catch {
      case e: Exception => None
    }
  }

}