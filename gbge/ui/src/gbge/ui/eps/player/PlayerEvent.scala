package gbge.ui.eps.player

import gbge.shared.FrontendPlayer
import org.scalajs.dom.WebSocket

abstract class PlayerEvent extends uiglue.Event
case object BootstrapPlayerEvent extends PlayerEvent
case class JoinWithName(name: String) extends PlayerEvent
case class RecoverTokenEvent(token: String) extends PlayerEvent
case class NewPlayerEvent(player: FrontendPlayer) extends PlayerEvent
case object SetupWSConnection extends PlayerEvent
case class RegisterWS(socket: WebSocket) extends PlayerEvent
abstract class ScreenEvent extends PlayerEvent
abstract class UIEvent extends PlayerEvent

case class CHANGE_TO_TAB(tab: Int) extends UIEvent