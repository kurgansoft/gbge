package gbge.shared.actions

import upickle.default.{macroRW, ReadWriter => RW}

sealed trait Action {
  val authenticationRequired: Boolean = true
  val systemOnly: Boolean = false
  val adminOnly: Boolean = false
  def serialize(): String
}

abstract class GameAction extends Action

abstract sealed class GeneralAction extends Action {
  override def serialize(): String = upickle.default.write[GeneralAction](this)
}

object GeneralAction {
  implicit def rw: RW[GeneralAction] = macroRW
}

abstract sealed class AdminAction extends GeneralAction {
  override val adminOnly: Boolean = true
}

object AdminAction {
  implicit def rw: RW[AdminAction] = macroRW
}

case class ProvideToken(token: String) extends GeneralAction {
  override val systemOnly: Boolean = true
}

object ProvideToken {
  implicit def rw: RW[ProvideToken] = macroRW
}

case class Join(name: String) extends GeneralAction {
  override val authenticationRequired: Boolean = false
}

object Join {
  implicit def rw: RW[Join] = macroRW
}

case class SelectGame(gameNumber: Int) extends AdminAction

object SelectGame {
  implicit def rw: RW[SelectGame] = macroRW
}

case object UnselectGame extends AdminAction

case object CancelGame extends AdminAction

case object Start extends AdminAction

case class KickPlayer(playerId: Int) extends GeneralAction

object KickPlayer {
  implicit def rw: RW[KickPlayer] = macroRW
}

case class LinkRoleToPlayer(playerId: Int, role: Int) extends GeneralAction

object LinkRoleToPlayer {
  implicit def rw: RW[LinkRoleToPlayer] = macroRW
}

case class UnlinkPlayerFromRole(playerId: Int) extends GeneralAction

object UnlinkPlayerFromRole {
  implicit def rw: RW[UnlinkPlayerFromRole] = macroRW
}

case class NaturalLink(roles: List[Int]) extends GeneralAction

object NaturalLink {
  implicit def rw: RW[NaturalLink] = macroRW
}

case class UnassignRole(roleId: Int) extends GeneralAction

object UnassignRole {
  implicit def rw: RW[UnassignRole] = macroRW
}

case class InvalidAction(payload: String) extends GeneralAction

object InvalidAction {
  implicit def rw: RW[InvalidAction] = macroRW
}

case object IncreaseProposedNumberOfPlayers extends AdminAction
case object DecreaseProposedNumberOfPlayers extends AdminAction