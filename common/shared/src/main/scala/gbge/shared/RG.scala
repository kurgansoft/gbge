package gbge.shared

import gbge.shared.actions.GameAction
import zio.json.JsonCodec

object RG {
  var gameCodecs: List[JsonCodec[FrontendGame[_ <: GameAction]]] = List.empty
}
