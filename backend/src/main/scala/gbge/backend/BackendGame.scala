package gbge.backend

import gbge.backend.models.Player
import gbge.shared.*
import gbge.shared.GameState.{IN_PROGRESS, NOT_STARTED}
import gbge.shared.actions.{Action, GameAction}
import zio.IO

import scala.language.implicitConversions

trait BackendGame[GR <: GameRole, GA <: GameAction, FG <: FrontendGame[GR, GA]]
    extends Game[GR, GA] {

  implicit def cnToTuple2(result: UniverseResult): (Any, UniverseResult) =
    (this, result)

  val noOfPlayers: Int

  protected def setActiveRoles(roles: Set[GR]): BackendGame[GR, GA, FG]

  def registerRoleById(roleId: Int): Either[Failure, BackendGame[GR, GA, FG]] =
    allPossibleRoles.find(_.roleId == roleId) match {
      case Some(role) => registerRole(role)
      case None       =>
        Left(GeneralFailure(s"Role with id [$roleId] does not exist."))
    }

  protected def registerRole(
      gameRole: GR
  ): Either[Failure, BackendGame[GR, GA, FG]] = {
    if (activeRoles.contains(gameRole))
      Right(this)
    else if (state == NOT_STARTED)
      Right(setActiveRoles(activeRoles + gameRole))
    else
      Left(
        GeneralFailure(
          "It is not possible to add new roles, once the game is started."
        )
      )
  }

  def unRegisterRoleByIdIfNeeded(roleId: Int): BackendGame[GR, GA, FG] = {
    allPossibleRoles.find(_.roleId == roleId) match {
      case None           => this
      case Some(gameRole) =>
        if (state == IN_PROGRESS)
          this
        else
          setActiveRoles(activeRoles - gameRole)
    }
  }

  def increaseRoomSize(): Either[
    Failure,
    (BackendGame[GR, GA, FG], IO[Nothing, Option[Action]])
  ] =
    Left(
      GeneralFailure("Increasing the room size is not supported in this game!")
    )

  def decreaseRoomSize(): Either[
    Failure,
    (BackendGame[GR, GA, FG], IO[Nothing, Option[Action]])
  ] =
    Left(
      GeneralFailure("Decreasing the room size is not supported in this game!")
    )

  def reduce(gameAction: GameAction, invoker: Player): Either[
    Failure,
    (BackendGame[GR, GA, FG], IO[Nothing, Option[Action]])
  ]

  def toFrontendGame(role: Option[Int] = None): FG

}
