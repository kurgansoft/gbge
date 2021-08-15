package gbge.ui.eps.portal

import gbge.client.{ClientResult, ExecuteEffect, NewFU, OK, UIState}
import gbge.shared.tm.{PlayerPerspective, SpectatorPerspective}
import gbge.ui.eps.player.{ClientState, NewPlayerEvent, StandardStateWrapper}
import gbge.ui.eps.spectator.{CONNECTED, SpectatorState}

abstract sealed class GeneralPortalClientState
case object ActionIsNotSelected extends GeneralPortalClientState
case object PerspectiveIsNotSelected extends GeneralPortalClientState
case object EverythingIsSelected extends GeneralPortalClientState
case object WaitingForInfo extends GeneralPortalClientState
case object MysteriousError extends GeneralPortalClientState

case class PortalState(
                        portalId: Option[Int] = None,
                        clientState: Option[UIState[_]] = None,
                        generalPortalClientState: GeneralPortalClientState = WaitingForInfo
                      ) extends UIState[PortalClientEvent] {

  implicit def implicitConversion(state: PortalState): (PortalState, ClientResult) = (state, OK)

  override def processClientEvent(event: PortalClientEvent): (PortalState, ClientResult) = {
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
            this.copy(clientState = Some(tempState), generalPortalClientState = EverythingIsSelected)
          }
          case PlayerPerspective(playerId) => {
            val player = frontendUniverse.players.find(_.id == playerId)
            if (player.isDefined) {
              val state: ClientState = StandardStateWrapper(ClientState().processClientEvent(NewPlayerEvent(player.get))._1).processClientEvent(NewFU(frontendUniverse))._1.clientState
              this.copy(clientState = Some(state), generalPortalClientState = EverythingIsSelected)
            } else {
              this.copy(clientState = None, generalPortalClientState = MysteriousError)
            }
          }
        }
      }
      case NewStateFromSubCommander(uiState) => {
        this.copy(clientState = Some(uiState))
      }
      case ActionNeedsToBeSelectedEvent => {
        this.copy(clientState = None, generalPortalClientState = ActionIsNotSelected)
      }
      case PerspectiveNeedsToBeSelectedEvent(_) => {
        this.copy(clientState = None, generalPortalClientState = PerspectiveIsNotSelected)
      }
    }
  }
}