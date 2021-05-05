package gbge.shared

import gbge.shared.actions.GameAction

case class FrontendUniverse(selectedGame: Option[Int] = None,
                            game: Option[FrontendGame[_ <: GameAction]] = None,
                            players: List[FrontendPlayer],
                            playerIdToRoleLink: Map[Int,Int] = Map.empty) {
  def serialize(): String = {
    val frontendGameAsString: String = if (game.isDefined) game.get.serialize() else ""
    upickle.default.write((selectedGame, frontendGameAsString, players, playerIdToRoleLink))
  }
}

object FrontendUniverse {
  def decode(raw: String): FrontendUniverse = {
    val temp = upickle.default.read[(Option[Int], String, List[FrontendPlayer], Map[Int, Int])](raw)
    val x: Option[FrontendGame[_ <: GameAction]] = if (temp._2 == "" || temp._1.isEmpty) None else {
      Some(RG.registeredGames(temp._1.get).decode(temp._2))
    }
    FrontendUniverse(temp._1, x, temp._3, temp._4)
  }
}