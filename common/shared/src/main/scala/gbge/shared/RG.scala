package gbge.shared

import gbge.shared.actions.GameAction
import zio.blocks.schema.json.JsonCodec

object RG {
  var gameCodecs: List[JsonCodec[FrontendGame[? <: GameAction]]] = List.empty
}
