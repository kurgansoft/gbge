package gbge.shared.tm

import gbge.shared.FrontendPlayer
import gbge.shared.actions.{Action, GameAction, GeneralAction}
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

case class EncodedActionInvokerAndPlayers(actionEncodedAsJson: String, invoker: Option[Int], players: List[FrontendPlayer]) {
  def convertToActionAndInvoker()(implicit actionCodecs: List[JsonCodec[_ <: GameAction]]): ActionInvokerAndPlayers = {
    val interpretedAsGeneralAction = GeneralAction.codec.decoder.decodeJson(actionEncodedAsJson)
    interpretedAsGeneralAction match {
      case Right(generalAction) => ActionInvokerAndPlayers(generalAction, invoker, players)
      case _ =>
        var index: Int = 0
        var action: Action = null

        while action == null && index < actionCodecs.size
        do {
          actionCodecs(index).decoder.decodeJson(actionEncodedAsJson) match {
            case Right(decodedAction) =>
              action = decodedAction
            case _ => ()
          }
          index += 1
        }

        if (action == null) {
          throw new RuntimeException(s"could not decode json value [$actionEncodedAsJson]")
        }
        ActionInvokerAndPlayers(action, invoker, players)
    }
  }
}

object EncodedActionInvokerAndPlayers {
  implicit val schema: Schema[EncodedActionInvokerAndPlayers] = DeriveSchema.gen
  implicit val jsonCodec: JsonCodec[EncodedActionInvokerAndPlayers] = DeriveJsonCodec.gen[EncodedActionInvokerAndPlayers]
}
