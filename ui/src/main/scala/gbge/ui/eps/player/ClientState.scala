package gbge.ui.eps.player

import gbge.client._
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.UIExport
import gbge.ui.state.OfflineState
import gbge.ui.state.screenstates.{JoinScreenState, WelcomeScreenState}
import org.scalajs.dom.WebSocket
import uiglue.{Event, EventLoop, UIState}
import upickle.default.{macroRW, ReadWriter => RW}
import zio.{ZIO, UIO}

import scala.language.implicitConversions

case class ClientState(
                        frontendUniverse: Option[FrontendUniverse] = None,
                        you: Option[FrontendPlayer] = None,
                        offlineState: OfflineState = JoinScreenState(),
                        tab: Int = 1, // 1 -> general, 2 -> meta, 3-> admin
                        ws: Option[WebSocket] = None
                      ) extends UIState[Event] {
  val isAdmin: Boolean = you.exists(_.isAdmin)

  def getCurrentGame: Option[UIExport] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  implicit def convert(clientState: ClientState): (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) =
    (clientState, _ => ZIO.succeed(List.empty))

  override def processEvent(event: Event): (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) = {
    event match {
      case DispatchActionWithToken(action) =>
        val theToken = you.flatMap(_.token)
        if (theToken.isDefined) {
          (this, ClientEffects.submitRestActionWithToken(action, theToken))
        } else {
          this
        }
      case NewFU(fu) => handleNewFU(fu)
      case BootstrapPlayerEvent =>
        (this, ClientEffects.recoverTokenEffect)
      case RecoverTokenEvent(token) =>
        (this, ClientEffects.getPlayerWithToken(token))
      case sa: ScreenEvent =>
        val x = offlineState.handleScreenEvent(sa, frontendUniverse, you)
        (this.copy(offlineState = x._1), x._2)
      case NewPlayerEvent(player) =>
        val temp = this.copy(you = Some(player))
        if (temp.offlineState.isInstanceOf[JoinScreenState]) {
          temp.copy(offlineState = WelcomeScreenState())
        } else {
          temp
        }
      case SetupWSConnection =>
        val token = this.you.flatMap(_.token)
        if (token.isDefined)
          (this, eh => ClientEffects.createWebSocketConnection(token, eh))
        else {
          this
        }
      case CHANGE_TO_TAB(tab) =>
        this.copy(tab = tab)
      case RegisterWS(webSocket) =>
        this.copy(ws = Some(webSocket))
      case _ =>
        this
    }
  }

  def handleNewFU(fu: FrontendUniverse): (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) = {

    val updatedYou = fu.players.find(_.id == this.you.get.id)
    val updatedYou2 = updatedYou.map(_.copy(token = you.get.token))
    val adminStatusLost: Boolean = you.exists(_.isAdmin) && updatedYou2.exists(!_.isAdmin)
    val newTab: Int = tab match {
      case 3|4|5|6 =>
        if (adminStatusLost)
          1
        else
          tab
      case _ => tab
    }
    if (updatedYou2.isEmpty) {
      this.copy(frontendUniverse = Some(fu), you = updatedYou2, offlineState = JoinScreenState(), tab = newTab)
    } else if (fu.game.isDefined) {
      val temp = this.copy(you = updatedYou2, tab = newTab)
      gbge.ui.RG.registeredGames(fu.selectedGame.get).handleNewFU(temp, fu)
    }
    else {
      this.offlineState match {
        case _ : WelcomeScreenState => this.copy(frontendUniverse = Some(fu), you = updatedYou2, tab = newTab)
        case _ => this.copy(frontendUniverse = Some(fu), you = updatedYou2, offlineState = WelcomeScreenState(), tab = newTab)
      }

    }
  }
}

abstract sealed class ACTIVE_MENU

object ACTIVE_MENU {
  implicit def rw: RW[ACTIVE_MENU] = macroRW
}

case object GAME_MENU extends ACTIVE_MENU {
  implicit def rw: RW[GAME_MENU.type] = macroRW
}
case object ADMIN_MENU extends ACTIVE_MENU {
  implicit def rw: RW[ADMIN_MENU.type] = macroRW
}
case object LOG_MENU extends ACTIVE_MENU {
  implicit def rw: RW[LOG_MENU.type] = macroRW
}
case object LOG_OUT_MENU extends ACTIVE_MENU {
  implicit def rw: RW[LOG_OUT_MENU.type] = macroRW
}
case object LOGIN_MENU extends ACTIVE_MENU {
  implicit def rw: RW[LOGIN_MENU.type] = macroRW
}