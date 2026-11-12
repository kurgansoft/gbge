package gbge.backend

import gbge.backend.models.Player
import gbge.shared.*
import gbge.shared.actions.{Action, GameAction}
import zio.IO

import scala.language.implicitConversions

trait BackendGame[GR <: GameRole, GA <: GameAction, FG <: FrontendGame[GR, GA]]
    extends Game[GR, GA] {

  implicit def cnToTuple2(result: UniverseResult): (Any, UniverseResult) =
    (this, result)

  val state: GameState

  val noOfPlayers: Int

  def roleIsAvailable(gameRole: GameRole): Either[String, Unit] = Right(())

  def increaseRoomSize(): Either[
    Failure,
    (BackendGame[? <: GR, ? <: GA, ? <: FG], IO[Nothing, Option[Action]])
  ] =
    Left(
      GeneralFailure("Increasing the room size is not supported in this game!")
    )

  def decreaseRoomSize(): Either[
    Failure,
    (BackendGame[? <: GR, ? <: GA, ? <: FG], IO[Nothing, Option[Action]])
  ] =
    Left(
      GeneralFailure("Decreasing the room size is not supported in this game!")
    )

  def reduce(gameAction: GameAction, invoker: Player): Either[
    Failure,
    (BackendGame[? <: GR, ? <: GA, ? <: FG], IO[Nothing, Option[Action]])
  ]

  def toFrontendGame(role: Option[Int] = None): FG

}
