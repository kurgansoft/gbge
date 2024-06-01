package chat.backend

import chat.shared.{AbstractChatGame, ChatAction, ClientChatGame, Message, SendMessage}
import gbge.backend.{BackendGame, GeneralFailure, OK, Player, Startable, UnauthorizedFailure, UniverseResult}
import gbge.shared.{DecodeCapable, FrontendGame, GameState, IN_PROGRESS}
import gbge.shared.actions.{Action, GameAction, NaturalLink}

case class ChatGame(override val messages: List[Message] = List.empty) extends AbstractChatGame with BackendGame[ClientChatGame] {
  override val state: GameState = IN_PROGRESS

  override val noOfPlayers: Int = 6

  override def reduce(gameAction: GameAction, invoker: Option[Player]): (BackendGame[ClientChatGame], UniverseResult) = {
    gameAction match {
      case chatAction: ChatAction => reduce0(chatAction, invoker.flatMap(_.role))
      case _ => (this, GeneralFailure("Improper action"))
    }
  }

  def reduce0(chatAction: ChatAction, invokerRole: Option[Int]): (ChatGame, UniverseResult) = {
    chatAction match {
      case SendMessage(message) => {
        invokerRole match {
          case None => (this, UnauthorizedFailure())
          case Some(roleNumber) => (this.copy(messages = messages.appended(Message(roleNumber, message))), OK)
        }
      }
    }
  }

  override def toFrontendGame(role: Option[Int]): ClientChatGame = ClientChatGame(messages)
}

object ChatGame extends Startable {
  override def start(noOfPlayers: Int): (BackendGame[_ <: FrontendGame[_ <: GameAction]], Option[Action]) = {
    (ChatGame(), Some(NaturalLink((1 to Math.min(noOfPlayers, 6)).toList)))
  }

  override val frontendGame: DecodeCapable = ClientChatGame
  override val name: String = "Chat"
}
