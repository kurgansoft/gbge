package gbge.ui.eps.tm

import gbge.client.UIState
import gbge.shared.FrontendPlayer
import gbge.shared.actions.Action
import gbge.shared.tm.{Perspective, PlayerPerspective, SpectatorPerspective}
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.{Screens0, SpectatorState}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all.{pre, _}
import japgolly.scalajs.react.{Callback, ScalaComponent}
import org.scalajs.dom.html
import org.scalajs.dom.html.Div

object Displayer {
  val tmComponent = ScalaComponent.builder[(TimeMachineState, TMCommander)]("TMComponent").
    render_P(t => {
      val tmState = t._1
      val commander = t._2
      div(display:="flex", flexDirection:="row", position:="fixed",
        actionPaneDisplayer(tmState.status, tmState.timeMachine.actions, tmState.selectedAction)(commander),
        div(display:="flex", flexDirection:="row",
          perspectiveChooser(tmState.selectedAction, tmState.selectedPerspective, tmState.getPlayersForSelectedAction)(commander),
          Option.when(tmState.selectedPerspective.isDefined)
          (perspectiveDisplayer(tmState.selectedAction, tmState.selectedPerspective, tmState.componentDisplayMode, tmState.selectedClientState)(commander))
        )(position:="fixed", left:="550px", top:="0px", right:="0px", bottom:="0px"),
        button("SAVE", onClick --> Callback {
          commander.addAnEventToTheEventQueue(SAVE)
        })(position:="fixed", bottom:="0px", right:="0px"),
        Option.when(tmState.portalId.isDefined)
        (a(href:="./portal.html#" + tmState.portalId.get, target:="_blank", "PORTAL ID: " + tmState.portalId.get)
          (position:="fixed", top:="0px", right:="0px")
        )
      )
    } ).build

  def perspectiveChooser(selectedAction: Option[Int], selectedPerspective: Option[Perspective], players: Option[List[FrontendPlayer]])(implicit commander: TMCommander): TagOf[html.Div] = {
    if (selectedAction.isEmpty)
      div(color:="yellow", "Choose an action on the left.")
    else {
      if (players.isEmpty) {
        div(color:="yellow", "Loading perspectives...")
      } else {
        div(color := "yellow", width := "200px", minWidth := "200px", borderRight := "solid 3px brown",
          div("The spectator", onClick --> Callback {
            commander.addAnEventToTheEventQueue(PerspectiveSelected(SpectatorPerspective))
          })(Option.when(selectedPerspective.map(_.id).contains(0))(border := "solid 1px green").toTagMod),
          (for (player <- players.get.sortBy(_.id)) yield {
            div(player.id + ". - " + player.name, onClick --> Callback {
              commander.addAnEventToTheEventQueue(PerspectiveSelected(PlayerPerspective(player.id)))
            })(Option.when(selectedPerspective.map(_.id).contains(player.id))(border := "solid 1px green").toTagMod)
          }).toTagMod,
          button(`type` := "button", `class` := "btn btn-primary", "RESET", onClick --> Callback {
            commander.addAnEventToTheEventQueue(ResetTmToNumber(selectedAction.get))
          })
        )
      }
    }
  }

  def actionPaneDisplayer(status: DataFetchStatus, actions: List[(Action, Boolean)], selectedAction: Option[Int])(implicit commander: TMCommander): TagOf[Div] = {
    status match {
      case LOADING => div(color:="yellow", "LOADING ACTIONS...")
      case LOADED => loadedActions(actions, selectedAction)
      case LOADING_FAILED => div(color:="yellow", "Failed to load actions :-(")
    }
  }

  def loadedActions(actions: List[(Action, Boolean)], selectedAction: Option[Int])(implicit commander: TMCommander): TagOf[html.Div] = {
    div(position:="fixed", height:="100%", overflowY:="auto",
      actionRow(0, selected = selectedAction.contains(0)),
      (for (action <- actions.zipWithIndex) yield {
        val index = action._2 + 1
        val selected = selectedAction.contains(index)
        actionRow(index, Some(action._1), selected)
      }).toTagMod
    )
  }

  def actionRow(number: Int, action: Option[(Action, Boolean)] = None, selected: Boolean = false)(implicit commander: TMCommander): TagOf[html.Div] = {
    val actionRepresentation = if (action.isDefined) {
      action.get._1.toString
    } else {
      "------------"
    }

    val theColor = if (action.exists(_._2 == false)) "red" else "yellow"

    div(display:="flex", flexDirection:="row", color:= theColor, onClick --> Callback {
      commander.addAnEventToTheEventQueue(ActionSelected(number))
    },
      div(actionRepresentation, width:="500px", borderRight:= "solid 2px green"),
      div(number + ".", width:="30px")
    )(Option.when(selected)(border:= "solid 2px blue").toTagMod)
  }

  def perspectiveDisplayer(actionNumber: Option[Int], perspective: Option[Perspective], componentDisplayMode: ComponentDisplayMode, selectedClientState: Either[CSState, UIState[_]] = Left(CS_NOT_SELECTED))(implicit commander: TMCommander): TagOf[html.Div] = {
    if (actionNumber.isDefined && perspective.isEmpty)
      div(color:="yellow", "Choose a perspective!")
    else if (actionNumber.isEmpty || perspective.isEmpty)
      div(color:="yellow", "Both an action and a perspective has to be selected!")
    else {

      if (selectedClientState.isRight)
        div(width:="100%",
          displayModeSelector(componentDisplayMode),
          div(position:="relative", top:="0px", bottom:="0px", left:="0px", right:="0px", height:="95%", overflow:="auto",
            innerPerspectiveDisplayer(selectedClientState.getOrElse(null), componentDisplayMode)
          )
        )
      else {
        selectedClientState.swap.getOrElse(null) match {
          case CS_NOT_SELECTED => div(color:="yellow", "...NOT_SELECTED...")
          case CS_LOADING => div(color:="yellow", "...LOADING...")
          case CS_LOADING_FAILED => div(color:="yellow", "...LOADING FAILED...")
        }
      }
    }
  }

  def displayModeSelector(componentDisplayMode: ComponentDisplayMode)(implicit commander: TMCommander): TagOf[html.Div] = {
    div(color:="yellow", display:="flex", flexDirection:="row", justifyContent:="center",
      div("PPRINT", onClick --> Callback {
        commander.addAnEventToTheEventQueue(SetComponentDisplayMode(PPRINT))
      })(Option.when(componentDisplayMode == PPRINT)(textDecoration:="underline").toTagMod),
      div("|", textAlign:="center", width:="35px"),
      div("COMPONENT", onClick --> Callback {
        commander.addAnEventToTheEventQueue(SetComponentDisplayMode(COMPONENT))
      })(Option.when(componentDisplayMode == COMPONENT)(textDecoration:="underline").toTagMod)
    )
  }

  def innerPerspectiveDisplayer(uiState: UIState[_], componentDisplayMode: ComponentDisplayMode)(implicit commander: TMCommander): TagOf[html.Div] = {
    if (componentDisplayMode == PPRINT) {
      div(
        pre(color:="yellow", pprint.apply(uiState).plainText)
      )
    } else {
      uiState match {
        case cs: ClientState => {
          div(
            gbge.ui.display.Displayer.displayer(cs, commander.createTMSubCommander())
          )
        }
        case ss: SpectatorState => {
          div(
            Screens0.root(ss, commander.createTMSubCommander())
          )
        }
        case _ => div("some error")
      }
    }
  }
}
