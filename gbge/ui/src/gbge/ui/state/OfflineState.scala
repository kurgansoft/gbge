package gbge.ui.state

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.eps.player.{PlayerEvent, ScreenEvent}
import zio.UIO

import scala.language.implicitConversions

trait OfflineState {

  implicit def implicitConversion[OS <: OfflineState, E <: uiglue.Event](offlineState: OS): (OS, UIO[List[E]]) = (offlineState, UIO.succeed(List.empty[E]))

  def handleScreenEvent(
                          sa: ScreenEvent,
                          fu: Option[FrontendUniverse] = None,
                          you: Option[FrontendPlayer] = None):
  (OfflineState, UIO[List[PlayerEvent]])
}

case object EmptyOfflineState extends OfflineState {
  override def handleScreenEvent(sa: ScreenEvent, fu: Option[FrontendUniverse], you: Option[FrontendPlayer]): (OfflineState, UIO[List[Nothing]]) = {
    println("The current screen has no state associated with it." +
      " Therefore no action can have any effect on it.")
    this
  }
}




