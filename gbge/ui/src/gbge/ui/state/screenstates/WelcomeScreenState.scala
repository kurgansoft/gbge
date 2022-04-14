package gbge.ui.state.screenstates

import gbge.shared.actions.SelectGame
import gbge.client._
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState
import zio.UIO

abstract sealed class WelcomeScreenEvent extends ScreenEvent
case class ChangeGameDropDownIndexEvent(index: Int) extends WelcomeScreenEvent
case object SelectGameEvent extends WelcomeScreenEvent

case class WelcomeScreenState(index: Int = 0) extends OfflineState {

  implicit def convert (welcomeScreenState: WelcomeScreenState): (WelcomeScreenState, UIO[List[ScreenEvent]]) =
    (welcomeScreenState, UIO.succeed(List.empty))

  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (WelcomeScreenState, UIO[List[ScreenEvent]]) = sa match {
    case wsa: WelcomeScreenEvent => reduce0(wsa, you.flatMap(_.token))
    case _ => this
  }

  def reduce0(action: WelcomeScreenEvent, token: Option[String]): (WelcomeScreenState, UIO[List[ScreenEvent]]) = action match {
    case ChangeGameDropDownIndexEvent(index) => this.copy(index = index)
    case SelectGameEvent =>
      (this, ClientEffects.submitRestActionWithToken(SelectGame(index), token))
    case _ => this
  }
}
