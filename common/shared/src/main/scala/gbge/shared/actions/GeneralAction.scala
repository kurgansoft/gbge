package gbge.shared.actions

import zio.json._
import zio.schema.{DeriveSchema, Schema}

sealed trait GeneralAction extends Action {
  override def convertToJson(): String =
    this.toJson
}

object GeneralAction {
  implicit val codec: JsonCodec[GeneralAction] =
    DeriveJsonCodec.gen[GeneralAction]
  
  implicit val schema: Schema[GeneralAction] = DeriveSchema.gen
}

case class ProvideToken(token: String) extends GeneralAction {
  override val systemOnly: Boolean = true
}

case class Join(name: String) extends GeneralAction {
  override val systemOnly: Boolean = false
}

sealed trait AdminAction extends GeneralAction {
  override val adminOnly: Boolean = true
}

case class SelectGame(gameNumber: Int) extends GeneralAction with AdminAction

case object UnselectGame extends GeneralAction with AdminAction

case object CancelGame extends GeneralAction with AdminAction

case object Start extends GeneralAction with AdminAction

case class DelegateAdminRole(idOfTheNewAdmin: Int) extends GeneralAction with AdminAction

case class KickPlayer(playerId: Int) extends GeneralAction

case class LinkRoleToPlayer(playerId: Int, role: Int) extends GeneralAction

case class UnlinkPlayerFromRole(playerId: Int) extends GeneralAction

case class NaturalLink(roles: List[Int]) extends GeneralAction

case class UnassignRole(roleId: Int) extends GeneralAction

case class InvalidAction(payload: String) extends GeneralAction

case object IncreaseProposedNumberOfPlayers extends GeneralAction with AdminAction

case object DecreaseProposedNumberOfPlayers extends GeneralAction with AdminAction 