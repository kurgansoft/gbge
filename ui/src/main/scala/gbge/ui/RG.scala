package gbge.ui

import gbge.shared.FrontendGame
import gbge.shared.actions.GameAction
import zio.blocks.schema.json.JsonCodec

object RG {
  var registeredGames
      : List[ClientGameProps[? <: GameAction, ? <: FrontendGame[?]]] =
    List.empty

  lazy val gameCodecs: List[JsonCodec[FrontendGame[? <: GameAction]]] =
    registeredGames
      .map(_.gameCodec)
      .asInstanceOf[List[
        JsonCodec[FrontendGame[?]]
      ]] // DOES NOT work without explicit casting
}
