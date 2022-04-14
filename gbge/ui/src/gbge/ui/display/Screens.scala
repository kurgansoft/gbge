package gbge.ui.display

import gbge.client.DispatchActionWithToken
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import gbge.shared.actions._
import gbge.shared.FrontendPlayer
import gbge.ui.eps.player.{CHANGE_TO_TAB, ClientState}
import gbge.ui.state.screenstates._
import uiglue.{Event, EventLoop}

object Screens {

  def sbd(b: Boolean) = {
    if (b) {
      List(disabled:= true).toTagMod
    } else {
      List.empty.toTagMod
    }
  }

  def joinScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    assert(state.offlineState.isInstanceOf[JoinScreenState])
    val jss = state.offlineState.asInstanceOf[JoinScreenState]

    def fieldContentChangedCallback(e: ReactEventFromInput): Callback = Callback {
        eventHandler(NameInput(e.target.value))
    }

    val nameInput = input(`type`:= "text", maxLength:= 20, fontSize := "50", onChange ==> fieldContentChangedCallback)

    div(display:="flex", flexDirection:="column", alignItems:="center", textAlign:="center",
      br,h1(color:="yellow", "Please enter your (nick)name!"),br,
      nameInput,br,
      button(`class`:="btn btn-primary", "Join", onClick --> Callback {
        eventHandler(SubmitName)
      })(sbd(!jss.submitEnabled)),br,
      if (jss.errorMessage.isDefined) {
        div(position:= "relative", width:= "330px", backgroundColor:="red", jss.errorMessage.get,
          div(position:= "absolute", top:="3px", right:="3px", "X", onClick --> Callback{eventHandler(DismissErrorMessage)})
        )
      } else div()
    )
  }

  def metaScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    lazy val roleButton = button(`class`:="btn btn-primary", "ROLES", onClick --> Callback {
      eventHandler(CHANGE_TO_TAB(5))
    })
    div(color:="yellow", display:="flex", flexDirection:="column", alignItems:="center", overflowY:="auto",
      h1("META SCREEN"),
      button(`class`:="btn btn-primary", "Log out", onClick --> Callback {
        eventHandler(DispatchActionWithToken(KickPlayer(state.you.get.id)))
      }),br,
      Option.when(state.frontendUniverse.flatMap(_.game).isDefined)(div(roleButton, br)),
      h1("Game specific settings/actions:"),br,
      Option.when(state.getCurrentGame.isDefined)
      (state.getCurrentGame.get.metaExtension(state, eventHandler))
    )
  }

  def playerRoleScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    val you = state.you.get
    val players: List[FrontendPlayer] = state.frontendUniverse.map(_.players).get
    val game = state.frontendUniverse.get.game.get
    div(color:="yellow",
      button(`class`:="btn btn-primary", "<-", onClick --> Callback {
        eventHandler(CHANGE_TO_TAB(2))
      }),
      h1(color:="yellow", "PLAYER ROLE SCREEN", textAlign:="center"),
      GeneralDirectives.generalRoleChooserScreen(you, players, game, eventHandler)
    )
  }

  def kickScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    div(
      button(`class`:="btn btn-primary", "<-", onClick --> Callback {
        eventHandler(CHANGE_TO_TAB(3))
      }),
      div(display:="flex", flexDirection:="column", alignItems:="center",
        (for (player <- state.frontendUniverse.get.players.filterNot(_.isAdmin))
          yield div(
            button(`class`:="btn btn-primary", player.name, onClick --> Callback {
              eventHandler(DispatchActionWithToken(KickPlayer(player.id)))
            })(minWidth:="100px"), paddingBottom:="15px"
          )).toTagMod
      )
    )
  }

  def delegateAdminRoleScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    div(
      button(`class`:="btn btn-primary", "<-", onClick --> Callback {
        eventHandler(CHANGE_TO_TAB(3))
      }),
      h1(color:="yellow", "Here you can delegate your admin role to another player."),
      div(display:="flex", flexDirection:="column", alignItems:="center",
        (for (player <- state.frontendUniverse.get.players.filterNot(_.isAdmin))
          yield div(
            button(`class`:="btn btn-primary", player.name, onClick --> Callback {
              eventHandler(DispatchActionWithToken(DelegateAdminRole(player.id)))
            })(minWidth:="100px"), paddingBottom:="15px"
          )).toTagMod
      )
    )
  }

  def adminScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    val gameIsInProgress: Boolean = state.frontendUniverse.get.game.isDefined
    val startStop = if (gameIsInProgress) "QUIT GAME" else "START GAME"
    div(color:="yellow", display:="flex", flexDirection:="column", alignItems:="center",
      h1("ADMIN SCREEN", color:="yellow"),br,br,
      GeneralDirectives.buttonStack(
        button(`class`:="btn btn-primary", startStop, onClick --> Callback {
          if (gameIsInProgress)
            eventHandler(DispatchActionWithToken(CancelGame))
        }),
        button(`class`:="btn btn-primary", "KICK PLAYER", onClick --> Callback {
          eventHandler(CHANGE_TO_TAB(4))
        }),
        button(`class`:="btn btn-primary", "DELEGATE ADMIN ROLE", onClick --> Callback {
          eventHandler(CHANGE_TO_TAB(6))
        })
      ),
      br,br,br,
      state.getCurrentGame.map(_.adminDisplayer(state, eventHandler))
    )
  }

  def welcomeScreen(state: ClientState, eventHandler: EventLoop.EventHandler[Event]) = {
    assert(state.frontendUniverse.isDefined)
    assert(state.offlineState.isInstanceOf[WelcomeScreenState])
    assert(state.you.isDefined)
    val welcomeScreenState: WelcomeScreenState = state.offlineState.asInstanceOf[WelcomeScreenState]

    val revert = Callback {
      eventHandler(DispatchActionWithToken(UnselectGame))
    }

    val start = Callback {
      eventHandler(DispatchActionWithToken(Start))
    }

    val sg = state.frontendUniverse.get.selectedGame
    val name: String = state.you.map(_.name).get
    val games = gbge.shared.RG.registeredGames.map(_.name)
    val playerNames = state.frontendUniverse.get.players.map(_.name)

    if (state.you.exists(_.isAdmin)) {
      div(`class`:= "text-center", color:="yellow", overflowY:="auto", height:="100%",
        p("Hello " + name + "!" ),
        p("Please select a game, and start it when everyone is around!"),
        div(display:= "flex", flexDirection:= "row", justifyContent:= "space-evenly", alignItems:="center",
          div(
            if (sg.isEmpty)
              Directives.gamePicker(games, welcomeScreenState.index, eventHandler)
            else {
              Directives.selectedGame(sg)
            }
          ),
          div(
            people(playerNames)
          )
        ),br(),br(),br(),br(),
        sg.map(_ =>
          div(
            button(`class`:="btn btn-primary", "REVERT", onClick --> revert),br,br,
            button(`class`:="btn btn-primary", "START", onClick --> start), br, br
          ))
      )
    } else {
      div(`class`:= "text-center", color:="yellow",
        p("Hello " + name + "!" ),
        p("Just wait for the admin to start a game..."),
        div(display:= "flex", flexDirection:= "row", justifyContent:= "space-evenly", alignItems:="center",
          Directives.selectedGame(state.frontendUniverse.get.selectedGame),
          div(
            people(playerNames)
          )
        )
      )
    }
  }

  def people(a: List[String]) = {
    ul(`class`:="list-group",
      (for (person <- a) yield li(color:="black", `class`:="list-group-item", person)).toTagMod
    )
  }
}
