package gbge.ui.eps.spectator

import gbge.client.*
import gbge.client.events_and_effects.{ClientEffects, Connect, ConnectionBrokeDown, NewFU, ScreenEvent}
import gbge.shared.FrontendUniverse
import gbge.ui.ClientGameProps
import gbge.ui.eps.ConnectionStatus
import gbge.ui.eps.ConnectionStatus.*
import gbge.ui.state.*
import uiglue.EventLoop.EventHandler
import uiglue.{Event, UIState}
import zio.{Clock, UIO, ZIO}

import scala.language.implicitConversions

case class SpectatorState(
                           frontendUniverse: Option[FrontendUniverse] = None,
                           sseStreamStatus: ConnectionStatus = NOT_YET_ESTABLISHED,
                           offlineState: OfflineState[Any] = EmptyOfflineState,
                      ) extends UIState[uiglue.Event, Clock] {

  def getCurrentGame: Option[ClientGameProps[_,_]] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  implicit def convert(state: SpectatorState): (SpectatorState, EventHandler[Event] => UIO[List[Event]]) =
    (state, _ => ZIO.succeed(List.empty))

  override def processEvent(event: Event): (SpectatorState, EventHandler[Event] => ZIO[Clock, Nothing, List[Event]]) = event match {
    case NewFU(fu) =>
      val temp = this.copy(frontendUniverse = Some(fu), sseStreamStatus = CONNECTED)
      if (temp.getCurrentGame.isDefined) {
        temp.getCurrentGame.get.handleNewFUForSpectator(temp, fu)
      } else {
        temp
      }
    case Connect(optionalComment) => (this, eh => for {
      _ <- ZIO.log("SpectatorState is attempting to subscribe to SSE stream.")
      _ <- ClientEffects.createSSEConnection(eh, comment = optionalComment).orDie
    } yield List.empty)
    case ConnectionBrokeDown(_) => this.copy(sseStreamStatus = BROKEN)
    case se : ScreenEvent =>
      val temp = offlineState.handleScreenEvent(se, frontendUniverse)
      (this.copy(offlineState = temp._1), temp._2)
    case _ => this
  }
}