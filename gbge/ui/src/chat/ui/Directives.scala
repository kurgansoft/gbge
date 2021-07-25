package chat.ui

import gbge.shared.FrontendPlayer
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.html.Div

object Directives {
  def messageDisplayer(name: String, message: String): TagOf[Div] = {
    div(color:="yellow", fontSize:="xx-large", border:="solid 1px blue", marginBottom:="10px", paddingLeft:="10px", name + " - " + message)
  }

  def players(players: List[FrontendPlayer]): TagOf[Div] = {
    val playersWithTheirRole = players.map(x => {
      x.role match {
        case None => x.name
        case Some(role) => x.name + " - Speaker" + x.role.get
      }
    })
    div(color:="yellow",
      h1(textAlign:="center", textDecoration:="underline", "PLAYERS:"),
      playersWithTheirRole.map(div(fontSize:="xx-large", border:="solid 1px green", marginBottom:="10px", paddingLeft:="10px", _)).toTagMod
    )
  }
}
