package chat.ui

import gbge.client.GeneralEvent
import gbge.shared.FrontendUniverse
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState
import zio.UIO

case object ChatOfflineState extends OfflineState {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], playerId: Option[Int]): (OfflineState, UIO[List[GeneralEvent]]) = {
    println("The current screen has no state associated with it." +
      " Therefore no action can have any effect on it.")
    this
  }
}
