package gbge.ui.state.screenstates

import gbge.shared.actions.SelectGame
import gbge.client._
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState

abstract sealed class WelcomeScreenEvent extends ScreenEvent
case class ChangeGameDropDownIndexEvent(index: Int) extends WelcomeScreenEvent
case object SelectGameEvent extends WelcomeScreenEvent

case class WelcomeScreenState(index: Int = 0) extends OfflineState {

  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (OfflineState, ClientResult) = sa match {
    case wsa: WelcomeScreenEvent => reduce0(wsa)
    case _ => this
  }

  def reduce0(action: WelcomeScreenEvent): (WelcomeScreenState, ClientResult) = action match {
    case ChangeGameDropDownIndexEvent(index) => (this.copy(index = index), OK)
    case SelectGameEvent => {
      (this, PrepareRestActionWithToken(SelectGame(index)))
    }
    case _ => (this, OK)
  }
}
