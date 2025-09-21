package gbge.ui.eps.player

import gbge.shared.JoinResponse

sealed trait PlayerEvent extends uiglue.Event
case object BootstrapPlayerEvent extends PlayerEvent
case class JoinWithName(name: String) extends PlayerEvent
case object CheckForTokenEvent extends PlayerEvent
case class RecoverTokenEvent(token: String) extends PlayerEvent
case class PlayerRecovered(id: Int, token: String) extends PlayerEvent
case class JoinResponseEvent(joinResponse: JoinResponse) extends PlayerEvent
case object CreateSSEStream extends PlayerEvent
abstract class ScreenEvent extends PlayerEvent
abstract class UIEvent extends PlayerEvent

case class CHANGE_TO_TAB(tab: Int) extends UIEvent