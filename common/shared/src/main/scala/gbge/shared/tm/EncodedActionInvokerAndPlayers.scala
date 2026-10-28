package gbge.shared.tm

import gbge.shared.FrontendPlayer
import gbge.shared.actions.{Action, GameAction, GeneralAction}
import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class EncodedActionInvokerAndPlayers(
    actionEncodedAsJson: String,
    invoker: Option[Int],
    players: List[FrontendPlayer]
) {
  def convertToActionAndInvoker()(implicit
      actionCodecs: List[JsonCodec[? <: GameAction]]
  ): ActionInvokerAndPlayers = {
    val interpretedAsGeneralAction =
      GeneralAction.jsonCodec.decode(actionEncodedAsJson)
    interpretedAsGeneralAction match {
      case Right(generalAction) =>
        ActionInvokerAndPlayers(generalAction, invoker, players)
      case Left(_) =>
        var index: Int = 0
        var action: Action = null

        while action == null && index < actionCodecs.size do {
          actionCodecs(index).decode(actionEncodedAsJson) match {
            case Right(decodedAction) =>
              action = decodedAction
            case Left(_) => ()
          }
          index += 1
        }

        if (action == null) {
          throw new RuntimeException(
            s"could not decode json value [$actionEncodedAsJson]"
          )
        }
        ActionInvokerAndPlayers(action, invoker, players)
    }
  }
}

object EncodedActionInvokerAndPlayers {
  val jsonCodec: JsonCodec[EncodedActionInvokerAndPlayers] =
    Schema.derived.jsonCodec
}
