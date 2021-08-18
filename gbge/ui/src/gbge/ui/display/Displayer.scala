package gbge.ui.display

import gbge.client.{ClientEvent, ClientEventHandler}
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.{Screens0, SpectatorState}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.all._

object Displayer {

  val rootComponent = ScalaComponent.builder[(ClientState, ClientEventHandler[ClientEvent])]("RootComponent")
    .render_P(t => {
      div(position:= "fixed", top:="0px", bottom:="0px", left:="0px", right:="0px",
        displayer(t._1, t._2)
      )
    })
    .build

  val spectatorRootComponent = ScalaComponent.builder[(SpectatorState, ClientEventHandler[ClientEvent])]("SpectatorRootComponent")
    .render_P(t => {
      div(position:= "fixed", top:="0px", bottom:="0px", left:="0px", right:="0px",
        Screens0.root(t._1, t._2)
      )
    })
    .build

  def displayer(state: ClientState, commander: ClientEventHandler[ClientEvent]) = {
    if (state.you.isEmpty)
      Screens.joinScreen(state, commander)
    else {
      val stateDiv = state.tab match {
        case 1 => {
          if (state.frontendUniverse.isEmpty) {
            div(color:="yellow", "LOADING...")
          } else if (state.frontendUniverse.get.game.isDefined) {
            state.getCurrentGame.get.playerDisplayer(state, commander)
          } else
            Screens.welcomeScreen(state, commander)
        }
        case 2 => Screens.metaScreen(state, commander)
        case 3 => Screens.adminScreen(state, commander)
        case 4 => Screens.kickScreen(state, commander)
        case 5 => Screens.playerRoleScreen(state, commander)
        case 6 => Screens.delegateAdminRoleScreen(state, commander)
      }
      div(position:= "absolute", top:= "0px", bottom:= "0px", left:= "0px", right:= "0px", display:="flex", flexDirection:="column",
        Directives.tabMenu(state, commander)(flex:="0 1 auto"),
        stateDiv(flex:="1 1 auto")
      )
    }
  }
}
