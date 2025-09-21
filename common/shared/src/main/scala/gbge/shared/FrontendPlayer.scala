package gbge.shared

import zio.json.*
import zio.schema.{DeriveSchema, Schema}

case class FrontendPlayer(id: Int,
                          name: String,
                          isAdmin: Boolean,
                          role: Option[Int] = None)

object FrontendPlayer {

  implicit val codec: JsonCodec[FrontendPlayer] =
    DeriveJsonCodec.gen[FrontendPlayer]

  implicit val schema: Schema[FrontendPlayer] = DeriveSchema.gen

  def getNameOfPlayerWithRole(roleNumber: Int)(implicit players: List[FrontendPlayer]): String = {
    players.find(_.role.contains(roleNumber)).map(_.name).getOrElse(MISSING_PLAYERS_NAME)
  }

  val MISSING_PLAYERS_NAME: String = "###MISSING_PLAYER###"
}
