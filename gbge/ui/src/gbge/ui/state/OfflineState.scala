package gbge.ui.state

import gbge.client._
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.ScreenEvent

trait OfflineState {

  implicit def implicitConversion(offlineState: OfflineState): (OfflineState, ClientResult) = (offlineState, OK)

  def handleScreenEvent(
                          sa: ScreenEvent,
                          fu: Option[FrontendUniverse] = None,
                          you: Option[FrontendPlayer] = None):
  (OfflineState, ClientResult)
}

case object EmptyOfflineState extends OfflineState {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (OfflineState, ClientResult) = {
    println("The current screen has no state associated with it." +
      " Therefore no action can have any effect on it.")
    this
  }
}




