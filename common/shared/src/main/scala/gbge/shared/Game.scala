package gbge.shared

import gbge.shared.actions.GameAction

trait Game[GR <: GameRole, GA <: GameAction] {
  val minPlayerNumber: Int
  val maxPlayerNumber: Int

  val roles: List[GR]

  def getRoleById(id: Int): Option[GR] = roles.find(_.roleId == id)

}
