package gbge.ui.eps.portal

import gbge.client.NewFU
import gbge.shared.tm.{PlayerPerspective, SpectatorPerspective}
import gbge.ui.eps.player.{ClientState, NewPlayerEvent}
import gbge.ui.eps.spectator.{CONNECTED, SpectatorState}
import uiglue.{Event, EventLoop, UIState}
import zio.{ZIO, UIO}

abstract sealed class GeneralPortalClientState
case object ActionIsNotSelected extends GeneralPortalClientState
case object PerspectiveIsNotSelected extends GeneralPortalClientState
case object EverythingIsSelected extends GeneralPortalClientState
case object WaitingForInfo extends GeneralPortalClientState
case object MysteriousError extends GeneralPortalClientState

case class PortalState(
                        portalId: Option[Int] = None,
                        clientState: Option[UIState[Event]] = None,
                        generalPortalClientState: GeneralPortalClientState = WaitingForInfo
                      ) extends UIState[PortalClientEvent] {

  implicit def implicitConversion(state: PortalState): (PortalState, EventLoop.EventHandler[PortalClientEvent] => UIO[List[PortalClientEvent]]) =
    (state, _ => ZIO.succeed(List.empty))

  override def processEvent(event: PortalClientEvent): (PortalState, EventLoop.EventHandler[PortalClientEvent] => UIO[List[PortalClientEvent]]) =
    event match {
      case Start =>
        (this, PortalEffects.retrievePortalIdFromHash)
      case PortalId(id) =>
        (this.copy(portalId = Some(id)), PortalEffects.setUpPortalWSConnection(id))
      case UniversePerspectivePairReceived(perspective, frontendUniverse) =>
        perspective match {
          case SpectatorPerspective =>
            val tempState = SpectatorState(frontendUniverse = Some(frontendUniverse), wsConnectionStatus = CONNECTED).processEvent(NewFU(frontendUniverse))._1
            this.copy(clientState = Some(tempState), generalPortalClientState = EverythingIsSelected)
          case PlayerPerspective(playerId) =>
            val player = frontendUniverse.players.find(_.id == playerId)
            if (player.isDefined) {
              val state: ClientState = ClientState().processEvent(NewPlayerEvent(player.get))._1.processEvent(NewFU(frontendUniverse))._1
              this.copy(clientState = Some(state), generalPortalClientState = EverythingIsSelected)
            } else {
              this.copy(clientState = None, generalPortalClientState = MysteriousError)
            }
        }
      case EventFromSubState(event) =>
        this.copy(clientState = clientState.map(_.processEvent(event)._1))
      case ActionNeedsToBeSelectedEvent =>
        this.copy(clientState = None, generalPortalClientState = ActionIsNotSelected)
      case PerspectiveNeedsToBeSelectedEvent(_) =>
        this.copy(clientState = None, generalPortalClientState = PerspectiveIsNotSelected)
    }
}