package gbge.ui.state.screenstates

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.client._
import gbge.ui.eps.player.{PlayerEvent, ScreenEvent}
import gbge.ui.state.OfflineState
import zio.UIO

abstract sealed class JoinScreenEvent extends ScreenEvent
case object SubmitName extends JoinScreenEvent
case class NameInput(name: String) extends JoinScreenEvent
case object DismissErrorMessage extends JoinScreenEvent
case class ErrorInput(message: String) extends JoinScreenEvent

case class JoinScreenState(nameInput: String = "", submitEnabled: Boolean = false, errorMessage: Option[String] = None) extends OfflineState {

  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (OfflineState, UIO[List[PlayerEvent]]) = sa match {
    case wsa: JoinScreenEvent => reduce0(wsa)
    case _ => this
  }

  def reduce0(action: JoinScreenEvent): (OfflineState, UIO[List[PlayerEvent]]) = {
    action match {
      case NameInput(name) =>
        this.copy(nameInput = name, submitEnabled = name.nonEmpty)
      case SubmitName =>
        (this, ClientEffects.joinWithName(nameInput))
      case ErrorInput(message) =>
        this.copy(errorMessage = Some(message))
      case DismissErrorMessage => this.copy(errorMessage = None)
    }
  }
}