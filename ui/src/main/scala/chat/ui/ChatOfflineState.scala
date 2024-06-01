package chat.ui

import chat.shared.SendMessage
import gbge.client.ClientEffects
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.{PlayerEvent, ScreenEvent}
import gbge.ui.state.OfflineState
import zio.UIO

abstract sealed class OfflineChatEvent extends ScreenEvent
case class SendMessageEvent(message: String) extends OfflineChatEvent

case object ChatOfflineState extends OfflineState {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (OfflineState, UIO[List[PlayerEvent]]) = {
    sa match {
      case SendMessageEvent(message) => (this, ClientEffects.submitRestActionWithToken(SendMessage(message), you.flatMap(_.token)))
      case _ => this
    }
  }
}
