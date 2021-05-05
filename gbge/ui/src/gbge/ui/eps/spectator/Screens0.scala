package gbge.ui.eps.spectator

import gbge.client.{ClientEvent, ClientEventHandler}
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.display.Directives
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.html.Div

object Screens0 {

  def root(state: SpectatorState, commander: ClientEventHandler[ClientEvent]): TagOf[Div] = {
    implicit val players: List[FrontendPlayer] = state.frontendUniverse.map(_.players.toList).orNull

    state.wsConnectionStatus match {
      case NOT_YET_ESTABLISHED => div(h1("WS connection is NOT YET ESTABLISHED", color:="yellow"))
      case BROKEN => div(h1("WS connection is BROKEN", color:="yellow"))
      case CONNECTED => {
        if (state.frontendUniverse.flatMap(_.game).isEmpty) {
          if (state.frontendUniverse.isDefined)
            root1(state.frontendUniverse.get)
          else
            div()
        } else {
          gbge.ui.RG.registeredGames(state.frontendUniverse.get.selectedGame.get).spectatorDisplayer(state, commander)
        }
      }
    }
  }

  def gameDisplayer(selectedGame: Int, encodedGameState: String)(implicit players: List[FrontendPlayer]) = {
    div("GAME DISPLAYER - maybe it is in RG?")
  }

  def root1(fu: FrontendUniverse) = {
    div(position:= "absolute", top:="100px", bottom:="100px", width:="50%", marginLeft:="-25%", left:="50%",
      div(fontSize:="2.5vw", position:="absolute", bottom:="0px", top:="0px", left:="0px", right:="0px", display:="flex", flexDirection:="row",
        div(width:= "100%", border:="1px solid black", backgroundColor:= "#4c674a",
          display:="flex", flexDirection:="column", justifyContent:="center", textAlign:= "center",
          Directives.selectedGame(fu.selectedGame)),
        div(width:= "100%", border:="1px solid black", backgroundColor:= "#65b95f",
          br,div("Players:"),
          br,
          div(position:="relative", left:="30px",
            people(fu.players.toList)
          )
       )
      )
    )
  }

  def people(a: List[FrontendPlayer]) = {
    ul(
      (for (person <- a) yield {
        if (person.isAdmin)
          li(b(person.name))
        else
          li(person.name)
      }).toTagMod
    )
  }
}
