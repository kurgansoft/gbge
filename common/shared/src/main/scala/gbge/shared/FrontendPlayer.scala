package gbge.shared

import upickle.default.{macroRW, ReadWriter => RW}

case class FrontendPlayer(id: Int,
                          name: String,
                          isAdmin: Boolean,
                          role: Option[Int] = None,
                          token: Option[String] = None)

object FrontendPlayer {
  implicit def rw: RW[FrontendPlayer] = macroRW

  def getNameOfPlayerWithRole(roleNumber: Int)(implicit players: List[FrontendPlayer]): String = {
    players.find(_.role.contains(roleNumber)).map(_.name).getOrElse(MISSING_PLAYERS_NAME)
  }

  val MISSING_PLAYERS_NAME: String = "###MISSING_PLAYER###"
}
