package gbge.ui

import gbge.shared.FrontendUniverse
import gbge.client._
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.SpectatorState
import japgolly.scalajs.react.vdom.TagOf
import org.scalajs.dom.html.Div

trait UIExport {
  val playerDisplayer: (ClientState, ClientEventHandler[ClientEvent]) => TagOf[Div]
  val spectatorDisplayer: (SpectatorState, ClientEventHandler[ClientEvent]) => TagOf[Div]
  val handleNewFU: (ClientState, FrontendUniverse) => (ClientState, ClientResult)
  val handleNewFUForSpectator: (SpectatorState, FrontendUniverse) => (SpectatorState, ClientResult) = (state, fu) => {
    (state.copy(frontendUniverse = Some(fu)), OK)
  }
  val adminDisplayer: (ClientState, ClientEventHandler[ClientEvent]) => TagOf[Div]
  val metaExtension: (ClientState, ClientEventHandler[ClientEvent]) => TagOf[Div] = (_, _) => {
    import japgolly.scalajs.react.vdom.html_<^._
    <.div()
  }
}
