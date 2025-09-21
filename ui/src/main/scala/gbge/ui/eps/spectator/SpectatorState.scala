package gbge.ui.eps.spectator

import gbge.client.*
import gbge.shared.FrontendUniverse
import gbge.ui.ClientGameProps
import gbge.ui.eps.player.{CreateSSEStream, ScreenEvent}
import gbge.ui.state.*
import uiglue.EventLoop.EventHandler
import uiglue.{Event, UIState}
import zio.{UIO, ZIO}

import scala.language.implicitConversions

sealed trait SSEStatus
case object NOT_YET_ESTABLISHED extends SSEStatus
case object CONNECTED extends SSEStatus
case object BROKEN extends SSEStatus

case class SpectatorState(
                           frontendUniverse: Option[FrontendUniverse] = None,
                           sseStreamStatus: SSEStatus = NOT_YET_ESTABLISHED,
                           offlineState: OfflineState = EmptyOfflineState,
                      ) extends UIState[uiglue.Event] {

  def getCurrentGame: Option[ClientGameProps[_,_]] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  implicit def convert(state: SpectatorState): (SpectatorState, EventHandler[Event] => UIO[List[Event]]) =
    (state, _ => ZIO.succeed(List.empty))

  override def processEvent(event: Event): (SpectatorState, EventHandler[Event] => UIO[List[Event]]) = event match {
    case NewFU(fu) =>
      val temp = this.copy(frontendUniverse = Some(fu), sseStreamStatus = CONNECTED)
      if (temp.getCurrentGame.isDefined) {
        temp.getCurrentGame.get.handleNewFUForSpectator(temp, fu)
      } else {
        temp
      }
    case CreateSSEStream => (this, eh => {
      ClientEffects.createSSEConnection(eh)
      ZIO.succeed(List.empty)
    })
    case WebsocketConnectionBrokeDown => this.copy(sseStreamStatus = BROKEN)
    case se : ScreenEvent =>
      val temp = offlineState.handleScreenEvent(se, frontendUniverse)
      (this.copy(offlineState = temp._1), temp._2)
    case _ => this
  }
}