package gbge.ui

import gbge.shared.FrontendUniverse
import gbge.client._
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.SpectatorState
import japgolly.scalajs.react.vdom.TagOf
import org.scalajs.dom.html.Div
import japgolly.scalajs.react.vdom.all._

trait UIExport {
  val playerDisplayer: (ClientState, ClientEventHandler[ClientEvent]) => TagOf[Div] = (_,_) => {
    div(color:="yellow", fontSize:="xx-large", "playerDisplayer is not yet implemented.")
  }
  val spectatorDisplayer: (SpectatorState, ClientEventHandler[ClientEvent]) => TagOf[Div] = (_,_) => {
    div(color:="yellow", fontSize:="xx-large", "spectatorDisplayer is not yet implemented.")
  }
  val handleNewFU: (ClientState, FrontendUniverse) => (ClientState, ClientResult) = (clientState, fu) => {
    (clientState.copy(frontendUniverse = Some(fu)), OK)
  }
  val handleNewFUForSpectator: (SpectatorState, FrontendUniverse) => (SpectatorState, ClientResult) = (state, fu) => {
    (state.copy(frontendUniverse = Some(fu)), OK)
  }
  val adminDisplayer: (ClientState, ClientEventHandler[ClientEvent]) => TagOf[Div] = (_,_) => {
    div(color:="yellow", fontSize:="xx-large", "adminDisplayer is not yet implemented.")
  }
  val metaExtension: (ClientState, ClientEventHandler[ClientEvent]) => TagOf[Div] = (_, _) => {
    import japgolly.scalajs.react.vdom.html_<^._
    <.div()
  }
}
