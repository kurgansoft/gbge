package gbge.client.events_and_effects

import gbge.shared.FrontendUniverse
import gbge.shared.actions.Action
import uiglue.Event

sealed trait GeneralEvent extends Event

case class NewFU(fu: FrontendUniverse) extends GeneralEvent

case object Connect extends GeneralEvent
case class ConnectionBrokeDown(timeStamp: Long) extends GeneralEvent
case class ConnectionEstablished(timeStamp: Long) extends GeneralEvent

case class DispatchActionWithToken(action: Action) extends GeneralEvent