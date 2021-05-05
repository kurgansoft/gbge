package gbge.ui.eps.spectator

import gbge.client._
import gbge.shared.FrontendUniverse
import gbge.ui.UIExport
import gbge.ui.eps.player.{RegisterWS, ScreenEvent, SetupWSConnection}
import gbge.ui.state._
import org.scalajs.dom.raw.WebSocket

abstract sealed class WSConnectionStatus
case object NOT_YET_ESTABLISHED extends WSConnectionStatus
case object CONNECTED extends WSConnectionStatus
case object BROKEN extends WSConnectionStatus

case class SpectatorState(
                        frontendUniverse: Option[FrontendUniverse] = None,
                        wsConnectionStatus: WSConnectionStatus = NOT_YET_ESTABLISHED,
                        offlineState: OfflineState = EmptyOfflineState,
                        ws: Option[WebSocket] = None
                      ) extends UIState[ClientEvent] {

  def getCurrentGame: Option[UIExport] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  implicit def implicitConversion(state: SpectatorState): (SpectatorState, ClientResult) = (state, OK)

  override def processClientEvent(action: ClientEvent): (SpectatorState, ClientResult) = action match {
    case NewFU(fu) => {
      val temp = this.copy(frontendUniverse = Some(fu), wsConnectionStatus = CONNECTED)
      if (temp.getCurrentGame.isDefined) {
        temp.getCurrentGame.get.handleNewFUForSpectator(this, fu)
      } else {
        temp
      }
    }
    case SetupWSConnection => (this, ExecuteEffect(ClientEffects.createWebSocketConnection(None)))
    case WebsocketConnectionBrokeDown => this.copy(wsConnectionStatus = BROKEN)
    case RegisterWS(_) => this.copy(wsConnectionStatus = CONNECTED)
    case se : ScreenEvent => {
      val temp = offlineState.handleScreenEvent(se, frontendUniverse)
      (this.copy(offlineState = temp._1), temp._2)
    }
    case _ => this
  }
}