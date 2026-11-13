package chat.shared

import gbge.shared.GameState.IN_PROGRESS
import gbge.shared.{Game, GameRole, GameState}

abstract class AbstractChatGame(
    val messages: List[Message],
    override val activeRoles: Set[Speaker]
) extends Game[Speaker, ChatAction] {

  override val state: GameState = IN_PROGRESS

  override val minPlayerNumber: Int = 1
  override val maxPlayerNumber: Int = 6

  override val allPossibleRoles: Set[Speaker] =
    Set(Speaker(1), Speaker(2), Speaker(3), Speaker(4), Speaker(5), Speaker(6))
}
