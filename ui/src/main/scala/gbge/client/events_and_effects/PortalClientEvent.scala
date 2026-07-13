package gbge.client.events_and_effects

import gbge.shared.FrontendUniverse
import gbge.shared.tm.Perspective

sealed trait PortalClientEvent extends uiglue.Event
case object Start extends PortalClientEvent
case class PortalId(id: Int) extends PortalClientEvent
case class UniversePerspectivePairReceived(perspective: Perspective, frontendUniverse: FrontendUniverse) extends PortalClientEvent
case object ActionNeedsToBeSelectedEvent extends PortalClientEvent
case class PerspectiveNeedsToBeSelectedEvent(selectedAction: Int) extends PortalClientEvent

case class EventFromSubState(event: uiglue.Event) extends PortalClientEvent
