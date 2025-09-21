package gbge.shared

trait Game {
  val minPlayerNumber: Int
  val maxPlayerNumber: Int

  val roles: List[GameRole]

  def getRoleById(id: Int) : Option[GameRole] = roles.find(_.roleId == id)

}
