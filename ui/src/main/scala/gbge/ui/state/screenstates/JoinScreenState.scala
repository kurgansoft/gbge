package gbge.ui.state.screenstates

import gbge.client.*
import gbge.shared.FrontendUniverse
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState
import gbge.ui.token.TokenService
import uiglue.Event
import zio.ZIO

abstract sealed class JoinScreenEvent extends ScreenEvent
case object SubmitName extends JoinScreenEvent
case class NameInput(name: String) extends JoinScreenEvent
case object DismissErrorMessage extends JoinScreenEvent
case class ErrorInput(message: String) extends JoinScreenEvent

case class JoinScreenState(nameInput: String = "", submitEnabled: Boolean = false, errorMessage: Option[String] = None) extends OfflineState[TokenService] {

  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], playerId: Option[Int]): 
    (OfflineState[TokenService], ZIO[TokenService, Nothing, List[Event]]) = sa match {
      case wsa: JoinScreenEvent => reduce0(wsa)
      case _ => this
    }
  
  implicit def convert(offlineState: OfflineState[TokenService]): (OfflineState[TokenService], ZIO[TokenService, Nothing, List[Event]]) =
    (offlineState, ZIO.succeed(List.empty))


  def reduce0(action: JoinScreenEvent): (OfflineState[TokenService], ZIO[TokenService, Nothing, List[Event]]) = {
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