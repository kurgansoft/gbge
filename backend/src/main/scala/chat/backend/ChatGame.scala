package chat.backend

import chat.shared.*
import gbge.backend.*
import gbge.backend.models.Player
import gbge.shared.GameState
import gbge.shared.GameState.*
import gbge.shared.actions.{Action, GameAction}
import zio.{IO, ZIO}

case class ChatGame(
    override val messages: List[Message] = List.empty,
    override val activeRoles: Set[Speaker] = Set.empty
) extends AbstractChatGame(messages, activeRoles)
    with BackendGame[Speaker, ChatAction, ClientChatGame] {
  override lazy val state: GameState = IN_PROGRESS

  override val noOfPlayers: Int = 6

  override protected def setActiveRoles(roles: Set[Speaker]): ChatGame =
    this.copy(activeRoles = roles)

  override def registerRole(
      gameRole: Speaker
  ): Either[Failure, BackendGame[Speaker, ChatAction, ClientChatGame]] = {
    if (activeRoles.contains(gameRole))
      Right(this)
    else
      Right(setActiveRoles(activeRoles + gameRole))
  }

  override def reduce(
      gameAction: GameAction,
      invoker: Player
  ): Either[Failure, (ChatGame, IO[Nothing, Option[Action]])] = {
    gameAction match {
      case chatAction: ChatAction => reduce0(chatAction, invoker.role)
      case _                      =>
        Left(GeneralFailure("Provided action cannot be handled by this game."))
    }
  }

  private def reduce0(
      chatAction: ChatAction,
      invokerRole: Option[Int]
  ): Either[Failure, (ChatGame, IO[Nothing, Option[Action]])] = {
    chatAction match {
      case SendMessage(message) => {
        invokerRole match {
          case None             => Left(UnauthorizedFailure("..."))
          case Some(roleNumber) =>
            Right(
              (
                this.copy(messages =
                  messages.appended(Message(roleNumber, message))
                ),
                ZIO.none
              )
            )
        }
      }
    }
  }

  override def toFrontendGame(role: Option[Int]): ClientChatGame =
    ClientChatGame(messages, activeRoles)
}
