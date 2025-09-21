package gbge.ui

import gbge.shared.FrontendGame
import gbge.shared.actions.GameAction
import zio.json.JsonCodec

object RG {
  var registeredGames: List[ClientGameProps[_ <: GameAction, _ <: FrontendGame[_]]] = List.empty

  lazy val gameCodecs: List[JsonCodec[FrontendGame[_ <: GameAction]]] =
    registeredGames.map(_.gameCodec).asInstanceOf[List[JsonCodec[FrontendGame[_]]]] // DOES NOT work without explicit casting
}
