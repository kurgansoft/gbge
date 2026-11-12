package gbge.ui

import gbge.shared.{FrontendGame, GameRole}
import gbge.shared.actions.GameAction
import zio.blocks.schema.json.JsonCodec

object RG {
  var registeredGames: List[ClientGameProps[?, ?, ?]] =
    List.empty

  lazy val gameCodecs
      : List[JsonCodec[FrontendGame[? <: GameRole, ? <: GameAction]]] =
    registeredGames
      .map(_.gameCodec)
      .asInstanceOf[
        List[JsonCodec[FrontendGame[? <: GameRole, ? <: GameAction]]]
      ] // DOES NOT work without explicit casting
}
