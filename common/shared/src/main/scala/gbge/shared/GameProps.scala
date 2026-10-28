package gbge.shared

import gbge.shared.actions.GameAction
import sttp.client4.{Request, UriContext, basicRequest}
import zio.blocks.schema.json.JsonCodec

trait GameProps[GA <: GameAction, FG <: FrontendGame[GA]] {
  val name: String
  val urlFragment: String

  val actionCodec: JsonCodec[GA]
  val gameCodec: JsonCodec[FG]

  def createSttpRequestFromGameAction(
      gameAction: GameAction
  ): Option[Request[Either[String, String]]] = {
    val castedGameAction: GA = gameAction.asInstanceOf[GA]
    Some(
      basicRequest
        .post(uri"performAction/$urlFragment/")
        .body(actionCodec.encodeToString(castedGameAction))
    )
  }
}
