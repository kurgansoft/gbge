package gbge.shared.actions

import zio.blocks.schema.Schema
import zio.blocks.schema.json.JsonCodec

sealed trait GeneralAction extends Action {
  override lazy val asJsonString: String =
    GeneralAction.jsonCodec.encodeToString(this)
}

object GeneralAction {
  val jsonCodec: JsonCodec[GeneralAction] = Schema.derived.jsonCodec
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

case class SelectGame(gameNumber: Int) extends AdminAction

case object UnselectGame extends AdminAction

case object CancelGame extends AdminAction

case object Start extends AdminAction

case class DelegateAdminRole(idOfTheNewAdmin: Int) extends AdminAction

case class KickPlayer(playerId: Int) extends GeneralAction

case class LinkRoleToPlayer(playerId: Int, role: Int) extends GeneralAction

case class UnlinkPlayerFromRole(playerId: Int) extends GeneralAction

case class NaturalLink(roles: List[Int]) extends GeneralAction

case class UnassignRole(roleId: Int) extends GeneralAction

case class InvalidAction(payload: String) extends GeneralAction

case object IncreaseProposedNumberOfPlayers extends AdminAction

case object DecreaseProposedNumberOfPlayers extends AdminAction
