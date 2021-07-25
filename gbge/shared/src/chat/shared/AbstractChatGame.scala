package chat.shared

import gbge.shared.{Game, GameRole}

abstract class AbstractChatGame(val messages: List[Message] = List.empty) extends Game {
  override val minPlayerNumber: Int = 1
  override val maxPlayerNumber: Int = 6

  override val roles: List[GameRole] = List(Speaker(1), Speaker(2), Speaker(3), Speaker(4), Speaker(5), Speaker(6))
}
