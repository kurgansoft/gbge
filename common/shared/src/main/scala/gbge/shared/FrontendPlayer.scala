package gbge.shared

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

case class FrontendPlayer(
    id: Int,
    name: String,
    isAdmin: Boolean,
    role: Option[Int] = None
)

object FrontendPlayer {
  val codec: JsonCodec[FrontendPlayer] = Schema.derived.jsonCodec

  def getNameOfPlayerWithRole(
      roleNumber: Int
  )(implicit players: List[FrontendPlayer]): String = {
    players
      .find(_.role.contains(roleNumber))
      .map(_.name)
      .getOrElse(MISSING_PLAYERS_NAME)
  }

  val MISSING_PLAYERS_NAME: String = "###MISSING_PLAYER###"
}
