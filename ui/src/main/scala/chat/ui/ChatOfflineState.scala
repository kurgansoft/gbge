package chat.ui

import gbge.shared.FrontendUniverse
import gbge.ui.eps.player.ScreenEvent
import gbge.ui.state.OfflineState
import uiglue.Event
import zio.ZIO

case object ChatOfflineState extends OfflineState[Any] {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], playerId: Option[Int]): (OfflineState[Any], ZIO[Any, Nothing, List[Event]]) = {
    println("The current screen has no state associated with it." +
      " Therefore no action can have any effect on it.")
    (this, ZIO.succeed(List.empty)) 
  }
}
