package gbge.ui.state.screenstates

import gbge.client.*
import gbge.shared.FrontendUniverse
import gbge.shared.actions.SelectGame
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState
import uiglue.Event
import zio.{UIO, ZIO}

import scala.language.implicitConversions

sealed trait WelcomeScreenEvent extends ScreenEvent
case class ChangeGameDropDownIndexEvent(index: Int) extends WelcomeScreenEvent
case object SelectGameEvent extends WelcomeScreenEvent

case class WelcomeScreenState(index: Int = 0) extends OfflineState {

  implicit def convert(welcomeScreenState: WelcomeScreenState): (WelcomeScreenState, UIO[List[Event]]) =
    (welcomeScreenState, ZIO.succeed(List.empty))

  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], playerId: Option[Int]): (WelcomeScreenState, UIO[List[Event]]) = sa match {
    case wsa: WelcomeScreenEvent => reduce0(wsa, None)
    case _ => this
  }

  def reduce0(action: WelcomeScreenEvent, token: Option[String]): (WelcomeScreenState, UIO[List[Event]]) = action match {
    case ChangeGameDropDownIndexEvent(index) => this.copy(index = index)
    case SelectGameEvent =>
      (this, ZIO.succeed(List(DispatchActionWithToken(SelectGame(index)))))
    case _ => this
  }
}
