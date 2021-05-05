package gbge.ui.eps.player

import gbge.client._
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.UIExport
import gbge.ui.state.OfflineState
import gbge.ui.state.screenstates.{JoinScreenState, WelcomeScreenState}
import org.scalajs.dom.raw.WebSocket
import upickle.default.{macroRW, ReadWriter => RW}

case class ClientState(
                        frontendUniverse: Option[FrontendUniverse] = None,
                        you: Option[FrontendPlayer] = None,
                        offlineState: OfflineState = JoinScreenState(),
                        tab: Int = 1, // 1 -> general, 2 -> meta, 3-> admin
                        ws: Option[WebSocket] = None
                      ) extends UIState[PlayerEvent] {
  val isAdmin: Boolean = you.exists(_.isAdmin)

  implicit def implicitConversion(state: ClientState): (ClientState, ClientResult) = (state, OK)

  def getCurrentGame: Option[UIExport] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  override def processClientEvent(event: PlayerEvent): (ClientState, ClientResult)  = {
    event match {
      case BootstrapPlayerEvent => {
        (this, ExecuteEffect[ClientEvent](ClientEffects.recoverTokenEffect))
      }
      case RecoverTokenEvent(token) => {
        (this, ExecuteEffect(ClientEffects.getPlayerWithToken(token)))
      }
      case sa: ScreenEvent => {
        val x = offlineState.handleScreenEvent(sa, frontendUniverse, you)
        (this.copy(offlineState = x._1), x._2)
      }
      case NewPlayerEvent(player) => {
        val temp = this.copy(you = Some(player))
        if (temp.offlineState.isInstanceOf[JoinScreenState]) {
          temp.copy(offlineState = WelcomeScreenState())
        } else {
          temp
        }
      }
      case SetupWSConnection => {
        val token = this.you.flatMap(_.token)
        if (token.isDefined)
          (this, ExecuteEffect(ClientEffects.createWebSocketConnection(token)))
        else {
          this
        }
      }
      case CHANGE_TO_TAB(tab) => {
        this.copy(tab = tab)
      }
      case RegisterWS(webSocket) => {
        this.copy(ws = Some(webSocket))
      }
      case _ => {
        this
      }
    }
  }

  def handleNewFU(fu: FrontendUniverse): (ClientState, ClientResult) = {
    val updatedYou = fu.players.find(_.id == this.you.get.id)
    val updatedYou2 = updatedYou.map(_.copy(token = you.get.token))
    if (updatedYou2.isEmpty) {
      this.copy(frontendUniverse = Some(fu), you = updatedYou2, offlineState = JoinScreenState())
    } else if (fu.game.isDefined) {
      val temp = this.copy(you = updatedYou2)
      gbge.ui.RG.registeredGames(fu.selectedGame.get).handleNewFU(temp, fu)
    }
    else {
      this.offlineState match {
        case _ : WelcomeScreenState => this.copy(frontendUniverse = Some(fu), you = updatedYou2)
        case _ => this.copy(frontendUniverse = Some(fu), you = updatedYou2, offlineState = WelcomeScreenState())
      }

    }
  }
}

abstract sealed class ACTIVE_MENU

object ACTIVE_MENU {
  implicit def rw: RW[ACTIVE_MENU] = macroRW
}

case object GAME_MENU extends ACTIVE_MENU
case object ADMIN_MENU extends ACTIVE_MENU
case object LOG_MENU extends ACTIVE_MENU
case object LOG_OUT_MENU extends ACTIVE_MENU
case object LOGIN_MENU extends ACTIVE_MENU