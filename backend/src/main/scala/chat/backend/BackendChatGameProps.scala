package chat.backend

import chat.shared.{ChatAction, ChatGameProps, ClientChatGame}
import gbge.backend.{BackendGame, BackendGameProps}
import gbge.shared.actions.{Action, NaturalLink}

object BackendChatGameProps
    extends ChatGameProps
    with BackendGameProps[ChatAction, ClientChatGame] {

  override def start(
      noOfPlayers: Int
  ): (BackendGame[ChatAction, ClientChatGame], Option[Action]) = {
    (ChatGame(), Some(NaturalLink((1 to Math.min(noOfPlayers, 6)).toList)))
  }
}
