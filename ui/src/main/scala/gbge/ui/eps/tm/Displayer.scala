package gbge.ui.eps.tm

import gbge.shared.tm.{ActionInvokerAndPlayers, Perspective, PlayerPerspective, SpectatorPerspective}
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.{Screens0, SpectatorState}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all.*
import japgolly.scalajs.react.{Callback, ScalaComponent}
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import uiglue.EventLoop.EventHandler
import uiglue.{Event, EventLoop, UIState}

import scala.util.Try

object Displayer {
  val tmComponent = ScalaComponent.builder[(TimeMachineState, EventLoop.EventHandler[TMClientEvent])]("TMComponent").
    render_P(t => {
      val tmState = t._1
      val eventHandler = t._2
      div(display:="flex", flexDirection:="row", position:="fixed",
        actionPaneDisplayer(tmState.status, tmState.actionsAndInvokers, tmState.selectedAction)(eventHandler),
        div(display:="flex", flexDirection:="row",
          perspectiveChooser(tmState.selectedAction, tmState.selectedPerspective, tmState.actionsAndInvokers)(eventHandler),
          Option.when(tmState.selectedPerspective.isDefined)
          (perspectiveDisplayer(tmState.selectedAction, tmState.selectedPerspective, tmState.componentDisplayMode, tmState.selectedClientState)(eventHandler))
        )(position:="fixed", left:="550px", top:="0px", right:="0px", bottom:="0px"),
        button("SAVE", onClick --> Callback {
          eventHandler(SAVE)
        })(position:="fixed", bottom:="0px", right:="0px"),
//        Option.when(tmState.portalId.isDefined)
//        (a(href:="./portal.html#" + tmState.portalId.get, target:="_blank", "PORTAL ID: " + tmState.portalId.get)
//          (position:="fixed", top:="0px", right:="0px")
//        )
      )
    } ).build

  private def perspectiveChooser(selectedAction: Option[Int], selectedPerspective: Option[Perspective], actionInvokerAndPlayersList: List[ActionInvokerAndPlayers])(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[html.Div] = {
    selectedAction match {
      case None =>
        div(color:="yellow", "Choose an action on the left.")
      case Some(number) =>
        val players = number match {
          case 0 => List.empty
          case n => actionInvokerAndPlayersList(number - 1).players
        }
        div(color := "yellow", width := "200px", minWidth := "200px", borderRight := "solid 3px brown",
          div("The spectator", onClick --> Callback {
            eventHandler(PerspectiveSelected(SpectatorPerspective))
          })(Option.when(selectedPerspective.map(_.id).contains(0))(border := "solid 1px green").toTagMod),
          (for (player <- players) yield {
            div(s"${player.id}. - ${player.name}", onClick --> Callback {
              eventHandler(PerspectiveSelected(PlayerPerspective(player.id)))
            })(Option.when(selectedPerspective.map(_.id).contains(player.id))(border := "solid 1px green").toTagMod)
          }).toTagMod,
          button(`type` := "button", `class` := "btn btn-primary", "RESET", onClick --> Callback {
            eventHandler(ResetTmToNumber(selectedAction.get))
          })
        )
    }
  }

  def actionPaneDisplayer(status: DataFetchStatus, actionsAndInvokers: List[ActionInvokerAndPlayers], selectedAction: Option[Int])(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[Div] = {
    status match {
      case LOADING => div(color:="yellow", "LOADING ACTIONS...")
      case LOADED => loadedActions(actionsAndInvokers, selectedAction)
      case LOADING_FAILED => div(color:="yellow", "Failed to load actions :-(")
    }
  }

  def loadedActions(actions: List[ActionInvokerAndPlayers], selectedAction: Option[Int])(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[html.Div] = {
    div(position:="fixed", height:="100%", overflowY:="auto",
      actionRow(0, None, selected = selectedAction.contains(0)),
      (for (action <- actions.zipWithIndex) yield {
        val index = action._2 + 1
        val selected = selectedAction.contains(index)
        actionRow(index, Some(action._1), selected)
      }).toTagMod
    )
  }

  def actionRow(number: Int, action: Option[ActionInvokerAndPlayers] = None, selected: Boolean = false)(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[html.Div] = {
    val actionRepresentation = if (action.isDefined) {
      action.get._1.toString
    } else {
      "------------"
    }

//    val theColor = if (action.exists(_._2 == false)) "red" else "yellow"
    val theColor = "yellow"

    div(display:="flex", flexDirection:="row", color:= theColor, onClick --> Callback {
      eventHandler(ActionSelected(number))
    },
      div(actionRepresentation, width:="500px", borderRight:= "solid 2px green"),
      div(s"$number.", width:="30px")
    )(Option.when(selected)(border:= "solid 2px blue").toTagMod)
  }

  private def perspectiveDisplayer(actionNumber: Option[Int],
                                   perspective: Option[Perspective],
                                   componentDisplayMode: ComponentDisplayMode,
                                   selectedClientState: CSState = CS_NOT_SELECTED)(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[html.Div] = {
    if (actionNumber.isDefined && perspective.isEmpty)
      div(color := "yellow", "Choose a perspective!")
    else if (actionNumber.isEmpty || perspective.isEmpty)
      div(color := "yellow", "Both an action and a perspective has to be selected!")
    else {
      selectedClientState match {
        case CS_NOT_SELECTED => div(color := "yellow", "...NOT_SELECTED...")
        case CS_LOADING => div(color := "yellow", "...LOADING...")
        case CS_LOADING_FAILED => div(color := "yellow", "...LOADING FAILED...")
        case CSState_Loaded(state) =>
          div(width := "100%",
            displayModeSelector(componentDisplayMode),
            div(position := "relative", top := "0px", bottom := "0px", left := "0px", right := "0px", height := "95%", overflow := "auto",
              innerPerspectiveDisplayer(state, componentDisplayMode)
            )
          )
      }
    }
  }

  def displayModeSelector(componentDisplayMode: ComponentDisplayMode)(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[html.Div] = {
    div(color:="yellow", display:="flex", flexDirection:="row", justifyContent:="center",
      div("PPRINT", onClick --> Callback {
        eventHandler(SetComponentDisplayMode(PPRINT))
      })(Option.when(componentDisplayMode == PPRINT)(textDecoration:="underline").toTagMod),
      div("|", textAlign:="center", width:="35px"),
      div("COMPONENT", onClick --> Callback {
        eventHandler(SetComponentDisplayMode(COMPONENT))
      })(Option.when(componentDisplayMode == COMPONENT)(textDecoration:="underline").toTagMod)
    )
  }

  def innerPerspectiveDisplayer(uiState: UIState[_, _], componentDisplayMode: ComponentDisplayMode)(implicit eventHandler: EventHandler[TMClientEvent]): TagOf[html.Div] = {
    val spectatorBridge: EventLoop.EventHandler[Event] = event => {
      eventHandler(EventFromSelectedPerspective(event))
    }

    val playerBridge: EventLoop.EventHandler[Event] = event => {
      eventHandler(EventFromSelectedPerspective(event))
    }

    if (componentDisplayMode == PPRINT) {
      div(
        pre(color:="yellow", pprint.apply(uiState).plainText)
      )
    } else {
      uiState match {
        case cs: ClientState =>
          val t = Try(gbge.ui.display.Displayer.displayer(cs, playerBridge)) match {
            case scala.util.Success(value) => value
            case scala.util.Failure(exception) =>
              div(color:= "yellow",
                h1("Encountered some error"),
                hr,
                div(exception.getMessage),
                div(exception.getStackTrace.mkString("\n\t"))
              )
          }
          div(t)
        case ss: SpectatorState =>
          div(
            Screens0.root(ss, spectatorBridge)
          )
        case _ => div("some error")
      }
    }
  }
}
