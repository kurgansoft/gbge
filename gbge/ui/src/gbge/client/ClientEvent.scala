package gbge.client

import gbge.shared.FrontendUniverse
import gbge.shared.actions.Action

abstract class ClientEvent

abstract class GeneralEvent extends ClientEvent

case class NewFU(fu: FrontendUniverse) extends GeneralEvent
case object WebsocketConnectionBrokeDown extends GeneralEvent
case class DispatchActionWithToken(action: Action) extends GeneralEvent
