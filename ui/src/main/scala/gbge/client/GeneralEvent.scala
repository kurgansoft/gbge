package gbge.client

import gbge.shared.FrontendUniverse
import gbge.shared.actions.Action
import uiglue.Event

sealed trait GeneralEvent extends Event

case class NewFU(fu: FrontendUniverse) extends GeneralEvent
case object WebsocketConnectionBrokeDown extends GeneralEvent
case class DispatchActionWithToken(action: Action) extends GeneralEvent