package gbge.backend.models

import gbge.shared.FrontendPlayer

case class Player(id: Int, name: String, isAdmin: Boolean = false, role: Option[Int] = None) {
  def toFrontendPlayer(): FrontendPlayer = FrontendPlayer(id, name, isAdmin, role)
}
