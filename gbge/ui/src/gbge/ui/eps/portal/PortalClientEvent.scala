package gbge.ui.eps.portal

import gbge.client.{ClientEvent, UIState}
import gbge.shared.FrontendUniverse
import gbge.shared.tm.Perspective

abstract sealed class PortalClientEvent extends ClientEvent
case object Start extends PortalClientEvent
case class PortalId(id: Int) extends PortalClientEvent
case class UniversePerspectivePairReceived(perspective: Perspective, frontendUniverse: FrontendUniverse) extends PortalClientEvent
case class NewStateFromSubCommander(uiState: UIState[_ <: ClientEvent]) extends PortalClientEvent
case object ActionNeedsToBeSelectedEvent extends PortalClientEvent
case class PerspectiveNeedsToBeSelectedEvent(selectedAction: Int) extends PortalClientEvent