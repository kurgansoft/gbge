package gbge.ui.eps.portal

import gbge.client.{ClientResult, ExecuteEffect, NewFU, OK, UIState}
import gbge.shared.tm.{PlayerPerspective, SpectatorPerspective}
import gbge.ui.eps.player.{ClientState, NewPlayerEvent, StandardStateWrapper}
import gbge.ui.eps.spectator.{CONNECTED, SpectatorState}

case class PortalState(
                        portalId: Option[Int] = None,
                        clientState: Option[UIState[_]] = None
                      ) extends UIState[PortalClientEvent] {

  implicit def implicitConversion(state: PortalState): (PortalState, ClientResult) = (state, OK)

  override def processClientEvent(event: PortalClientEvent): (PortalState, ClientResult) =
    event match {
      case Start => {
        (this, ExecuteEffect(PortalEffects.retrievePortalIdFromHash))
      }
      case PortalId(id) => {
        (this.copy(portalId = Some(id)), ExecuteEffect(PortalEffects.setUpPortalWSConnection(id)))
      }
      case UniversePerspectivePairReceived(perspective, frontendUniverse) => {
        perspective match {
          case SpectatorPerspective => {
            val tempState = SpectatorState(frontendUniverse = Some(frontendUniverse), wsConnectionStatus = CONNECTED).processClientEvent(NewFU(frontendUniverse))._1
            this.copy(clientState = Some(tempState))
          }
          case PlayerPerspective(playerId) => {
            val player = frontendUniverse.players.find(_.id == playerId)
            if (player.isDefined) {
              val state: ClientState = StandardStateWrapper(ClientState().processClientEvent(NewPlayerEvent(player.get))._1).processClientEvent(NewFU(frontendUniverse))._1.clientState
              this.copy(clientState = Some(state))
            } else {
              this.copy(clientState = None)
            }
          }
        }
      }
      case NewStateFromSubCommander(uiState) => {
        this.copy(clientState = Some(uiState))
      }
    }
}