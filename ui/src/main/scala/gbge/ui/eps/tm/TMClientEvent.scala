package gbge.ui.eps.tm

import gbge.shared.{ClientTimeMachine, FrontendUniverse}
import gbge.shared.tm.{ActionInvokerAndPlayers, Perspective, TMMessage}

sealed trait TMClientEvent extends uiglue.Event

case class ActionsHaveArrived(actions: List[ActionInvokerAndPlayers]) extends TMClientEvent
case class TimeMachineHaveArrived(tm: ClientTimeMachine) extends TMClientEvent
case object TimeToPersistEvent extends TMClientEvent
case object TimeMachineRetrievalFailed extends TMClientEvent
case class ActionSelected(selection: Int) extends TMClientEvent
case class TMStateArrived(number: Int, perspective: Perspective, fu: FrontendUniverse) extends TMClientEvent
case class PerspectiveSelected(perspective: Perspective) extends TMClientEvent
case object SAVE extends TMClientEvent
case class SetComponentDisplayMode(componentDisplayMode: ComponentDisplayMode) extends TMClientEvent
case class SetPortalId(portalId: Int) extends TMClientEvent
case class TMMessageContainer(tmMessage: TMMessage) extends TMClientEvent
case class ResetTmToNumber(number: Int) extends TMClientEvent
case class TmGotShrunk(number: Int) extends TMClientEvent
case object Start extends TMClientEvent

case class RecoveredHash(hash: String) extends TMClientEvent
case object RetrieveMissingStates extends TMClientEvent

case class EventFromSelectedPerspective(event: uiglue.Event) extends TMClientEvent
