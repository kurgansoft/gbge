package chat.ui

import chat.shared.SendMessage
import gbge.client.{ClientResult, PrepareRestActionWithToken}
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState

abstract sealed class OfflineChatEvent extends ScreenEvent
case class SendMessageEvent(message: String) extends OfflineChatEvent

case object ChatOfflineState extends OfflineState {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (OfflineState, ClientResult) = {
    sa match {
      case SendMessageEvent(message) => (this, PrepareRestActionWithToken(SendMessage(message)))
      case _ => this
    }
  }
}
