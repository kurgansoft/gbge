package gbge.ui

import gbge.shared.FrontendUniverse
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.SpectatorState
import japgolly.scalajs.react.vdom.TagOf
import org.scalajs.dom.html.Div
import japgolly.scalajs.react.vdom.all._
import uiglue.{Event, EventLoop}
import zio.UIO

trait UIExport {
  val playerDisplayer: (ClientState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_,_) => {
    div(color:="yellow", fontSize:="xx-large", "playerDisplayer is not yet implemented.")
  }
  val spectatorDisplayer: (SpectatorState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_,_) => {
    div(color:="yellow", fontSize:="xx-large", "spectatorDisplayer is not yet implemented.")
  }
  val handleNewFU: (ClientState, FrontendUniverse) => (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) = (clientState, fu) => {
    (clientState.copy(frontendUniverse = Some(fu)), _ => UIO.succeed(List.empty))
  }
  val handleNewFUForSpectator: (SpectatorState, FrontendUniverse) => (SpectatorState, EventLoop.EventHandler[Event] => UIO[List[uiglue.Event]]) = (state, fu) => {
    (state.copy(frontendUniverse = Some(fu)), _ => UIO.succeed(List.empty[uiglue.Event]))
  }
  val adminDisplayer: (ClientState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_,_) => {
    div(color:="yellow", fontSize:="xx-large", "adminDisplayer is not yet implemented.")
  }
  val metaExtension: (ClientState, EventLoop.EventHandler[Event]) => TagOf[Div] = (_, _) => {
    import japgolly.scalajs.react.vdom.html_<^._
    <.div()
  }
}
