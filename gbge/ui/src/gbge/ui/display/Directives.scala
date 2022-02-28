package gbge.ui.display

import gbge.client.{ClientEvent, ClientEventHandler}
import gbge.ui.eps.player.{CHANGE_TO_TAB, ClientState}
import japgolly.scalajs.react.{Callback, ReactEventFrom}
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.HTMLSelectElement
import gbge.ui.state.screenstates.{ChangeGameDropDownIndexEvent, SelectGameEvent}

object Directives {

  def selectedGame(selectedGame: Option[Int]) = {
    if (selectedGame.isEmpty) {
      div("No game is selected just yet.")
    } else {
      val str = gbge.shared.RG.registeredGames(selectedGame.get).name
      div(border:="2px solid black", marginLeft:="10%", marginRight:="10%", str)
    }
  }

  def gamePicker(games: List[String], si: Int, commander: ClientEventHandler[ClientEvent]) = {
    def selectChangeCB(e: ReactEventFrom[HTMLSelectElement]): Callback = Callback {
      val index = e.target.value.toInt
      commander.addAnEventToTheEventQueue(ChangeGameDropDownIndexEvent(index))
    }
    div(
      select(onChange ==> selectChangeCB, defaultValue:= si,
        (for (x <- games.zipWithIndex) yield option(value:= x._2, x._1)).toTagMod),
      br,
      input(`type`:= "button", value:= "CHOOSE", onClick --> Callback {
        commander.addAnEventToTheEventQueue(SelectGameEvent)
      })
    )
  }

  def tabMenu(state: ClientState, commander: ClientEventHandler[ClientEvent]) = {
    def cb(index: Int) = Callback {
      commander.addAnEventToTheEventQueue(CHANGE_TO_TAB(index))
    }

    def td(index: Int) = if ((state.tab ==index) || (index == 3 && state.tab > 3)) "underline" else "none"

    div(display:="flex", flexDirection:= "row", justifyContent:= "space-evenly", color:= "white",
      div("Game", onClick --> cb(1), textDecoration:= td(1)),
      div("Meta", onClick --> cb(2), textDecoration:= td(2)),
      state.you.find(_.isAdmin).map(_ => div("Admin", onClick --> cb(3), textDecoration:= td(3)))
    )
  }
}
