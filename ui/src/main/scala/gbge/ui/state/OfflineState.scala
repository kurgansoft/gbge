package gbge.ui.state

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.{PlayerEvent, ScreenEvent}
import uiglue.Event
import zio.{UIO, ZIO}

import scala.language.implicitConversions

trait OfflineState[-D] {

  def handleScreenEvent(
                          sa: ScreenEvent,
                          fu: Option[FrontendUniverse] = None,
                          playerId: Option[Int] = None):
  (OfflineState[D], ZIO[D, Nothing, List[Event]])
}

case object EmptyOfflineState extends OfflineState[Any] {

  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], playerId: Option[Int]): (OfflineState[Any], ZIO[Any, Nothing, List[Event]]) = {
    println("The current screen has no state associated with it." +
      " Therefore no action can have any effect on it.")
    (this, ZIO.succeed(List.empty))
  }
}




