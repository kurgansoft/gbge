package gbge.ui.state

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.{PlayerEvent, ScreenEvent}
import uiglue.Event
import zio.{UIO, ZIO}

import scala.language.implicitConversions

trait OfflineState {

  implicit def implicitConversion[OS <: OfflineState, E <: uiglue.Event](offlineState: OS): (OS, UIO[List[E]]) = (offlineState, ZIO.succeed(List.empty[E]))

  def handleScreenEvent(
                          sa: ScreenEvent,
                          fu: Option[FrontendUniverse] = None,
                          playerId: Option[Int] = None):
  (OfflineState, UIO[List[Event]])
}

case object EmptyOfflineState extends OfflineState {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], playerId: Option[Int]): (OfflineState, UIO[List[Nothing]]) = {
    println("The current screen has no state associated with it." +
      " Therefore no action can have any effect on it.")
    this
  }
}




