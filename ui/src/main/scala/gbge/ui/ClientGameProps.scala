package gbge.ui

import gbge.shared.{FrontendGame, FrontendUniverse, GameProps}
import gbge.shared.actions.GameAction
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.SpectatorState
import japgolly.scalajs.react.vdom.TagOf
import org.scalajs.dom.html.Div
import uiglue.{Event, EventLoop}
import zio.{UIO, ZIO}
import japgolly.scalajs.react.vdom.all.*

trait ClientGameProps[GA <: GameAction, FG <: FrontendGame[GA]] extends GameProps[GA, FG] {
  val handleNewFU: (ClientState, FrontendUniverse) => (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) = (clientState, fu) => {
    (clientState.copy(frontendUniverse = Some(fu)), _ => ZIO.succeed(List.empty))
  }

  val handleNewFUForSpectator: (SpectatorState, FrontendUniverse) => (SpectatorState, EventLoop.EventHandler[Event] => UIO[List[uiglue.Event]]) = (state, fu) => {
    (state.copy(frontendUniverse = Some(fu)), _ => ZIO.succeed(List.empty[uiglue.Event]))
  }

  val playerDisplayer: (ClientState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_, _) => {
    div(color := "yellow", fontSize := "xx-large", "playerDisplayer is not yet implemented.")
  }

  val spectatorDisplayer: (SpectatorState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_, _) => {
    div(color := "yellow", fontSize := "xx-large", "spectatorDisplayer is not yet implemented.")
  }

  val adminDisplayer: (ClientState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_, _) => {
    div(color := "yellow", fontSize := "xx-large", "adminDisplayer is not yet implemented.")
  }

  val metaExtension: (ClientState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_, _) => {
    import japgolly.scalajs.react.vdom.html_<^._
    <.div()
  }
}
