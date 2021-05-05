package gbge.ui.display

import gbge.client.{ClientEvent, ClientEventHandler, DispatchActionWithToken}
import gbge.shared.{FrontendGame, FrontendPlayer}
import gbge.shared.actions.{LinkRoleToPlayer, UnlinkPlayerFromRole}
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.html.{Button, Div}

object GeneralDirectives {

  def generalRoleDisplayer(players: List[FrontendPlayer], frontendGame: FrontendGame[_]): TagOf[Div] = {
    val allTheRoles = frontendGame.roles
    div(display:="flex", flexDirection:="row", justifyContent:="space-evenly", width:="100%", textAlign:="left", color:= "black",
      div(u("Taken roles:"), backgroundColor:="#656565", width:="100%",
        (for (p <- players.filter(_.role.isDefined))
          yield div(p.name + " - " + allTheRoles.find(role => p.role.contains(role.roleId)).get)).toTagMod
      ),
      div(u("Players with no roles:"), backgroundColor:="#65b95f", width:="100%",
        (for (p <- players.filter(_.role.isEmpty))
          yield div(p.name)).toTagMod
      )
    )
  }

  def generalRoleChooserScreen(you: FrontendPlayer, players: List[FrontendPlayer], frontendGame: FrontendGame[_], commander: ClientEventHandler[ClientEvent]): TagOf[Div] = {
    val allTheRoles = frontendGame.roles
    val availableRoles = allTheRoles.filterNot(role2 => players.exists(_.role.contains(role2.roleId)))
    div(display:="flex", flexDirection:="column", alignItems:="center", textAlign:="center",
      if (you.role.isDefined) {
        div(
          br, div("Your role: " + allTheRoles.find(role => you.role.contains(role.roleId)).get), br, br,
          button(`class`:="btn btn-primary", "LEAVE YOUR ROLE", onClick --> Callback {
            commander.addAnEventToTheEventQueue(DispatchActionWithToken(UnlinkPlayerFromRole(you.id)))
          }),br,br
        )
      } else {
        div(
          div("Choose a role:"),
          availableRoles.map(gameRole =>
            div(
              button(`class`:="btn btn-primary", gameRole.toString, onClick --> Callback {
                commander.addAnEventToTheEventQueue(DispatchActionWithToken(LinkRoleToPlayer(you.id, gameRole.roleId)))
              }),
              br,br
            )
          ).toTagMod
        )
      },
      generalRoleDisplayer(players, frontendGame)
    )
  }

  def buttonStack(buttons: TagOf[Button]*): TagOf[Div] = {
    div(display:="flex", flexDirection:="column",
      buttons.map(_.apply(marginBottom:="30px")).toTagMod
    )
  }
}
