package gbge.shared

import gbge.shared.actions.GameAction

trait Game[GR <: GameRole, GA <: GameAction] {
  lazy val state: GameState

  val minPlayerNumber: Int
  val maxPlayerNumber: Int

  val allPossibleRoles: Set[GR]
  val activeRoles: Set[GR]
}
