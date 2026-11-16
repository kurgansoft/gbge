package gbge.ui.eps.spectator

import gbge.client.*
import gbge.client.events_and_effects.{
  ClientEffects,
  Connect,
  ConnectionBrokeDown,
  ConnectionEstablished,
  NewFU,
  ScreenEvent,
  SetupEventListeners,
  VisibilityChanged
}
import gbge.shared.FrontendUniverse
import gbge.ui.ClientGameProps
import gbge.ui.eps.ConnectionState
import gbge.ui.eps.ConnectionStatus.*
import gbge.ui.state.*
import uiglue.EventLoop.EventHandler
import uiglue.{Event, UIState}
import zio.{Clock, UIO, ZIO}

import scala.language.implicitConversions

case class SpectatorState(
    frontendUniverse: Option[FrontendUniverse] = None,
    connectionState: ConnectionState = ConnectionState(),
    offlineState: OfflineState[Any] = EmptyOfflineState
) extends UIState[uiglue.Event, Clock] {

  def getCurrentGame: Option[ClientGameProps[?, ?, ?]] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  implicit def convert(
      state: SpectatorState
  ): (SpectatorState, EventHandler[Event] => UIO[List[Event]]) =
    (state, _ => ZIO.succeed(List.empty))

  override def processEvent(
      event: Event
  ): (SpectatorState, EventHandler[Event] => ZIO[Clock, Nothing, List[Event]]) =
    event match {
      case VisibilityChanged =>
        (
          this,
          _ =>
            ZIO.succeed(
              if (
                connectionState.status == BROKEN &&
                org.scalajs.dom.document.visibilityState == "visible" &&
                connectionState.isItWorthToTryReconnecting
              )
                List(
                  Connect(
                    Some(
                      "RECONNECTING AFTER BROKEN CONNECTION AS SCREEN IS VISIBLE AGAIN"
                    )
                  )
                )
              else List.empty
            )
        )
      case SetupEventListeners =>
        (
          this,
          eh =>
            for {
              _ <- ZIO.log(
                "Setting up event listener for visibility change DOM event."
              )
              _ = org.scalajs.dom.window.addEventListener(
                "visibilitychange",
                (event: org.scalajs.dom.Event) => eh(VisibilityChanged)
              )
            } yield List.empty
        )
      case NewFU(fu) =>
        val temp = this.copy(frontendUniverse = Some(fu))
        if (temp.getCurrentGame.isDefined) {
          temp.getCurrentGame.get.handleNewFUForSpectator(temp, fu)
        } else {
          temp
        }
      case Connect(optionalComment) =>
        (
          this,
          eh =>
            for {
              _ <- ZIO.log(
                "SpectatorState is attempting to subscribe to SSE stream."
              )
              _ <- ClientEffects
                .createSSEConnection(eh, comment = optionalComment)
                .orDie
                .forkDaemon
            } yield List.empty
        )
      case ConnectionBrokeDown(timeStamp) =>
        val newConnectionState =
          connectionState.addDisconnectionTimeStamp(timeStamp)
        val eventList =
          if (newConnectionState.isItWorthToTryReconnecting)
            List(Connect(Some("STANDARD RECONNECTION AFTER DISCONNECT")))
          else List.empty
        (
          this.copy(connectionState = newConnectionState),
          _ =>
            for {
              _ <- ZIO.when(eventList.nonEmpty)(
                ZIO.log(
                  "Connection just broke down, attempting to reconnect..."
                )
              )
              _ <- ZIO.when(eventList.isEmpty)(
                ZIO.log(
                  "Connection just broke down, but _NOT_ worth reconnecting."
                )
              )
            } yield eventList
        )
      case ConnectionEstablished(timeStamp) =>
        this.copy(connectionState =
          connectionState.transitionToConnectedStateWithTimeStamp(timeStamp)
        )
      case se: ScreenEvent =>
        val temp = offlineState.handleScreenEvent(se, frontendUniverse)
        (this.copy(offlineState = temp._1), temp._2)
      case _ => this
    }
}
