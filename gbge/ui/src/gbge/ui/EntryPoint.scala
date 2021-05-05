package gbge.ui

import gbge.ui.eps.player.{Commander, StandardStateWrapper}
import gbge.ui.eps.portal.{PortalCommander, PortalState}
import gbge.ui.eps.spectator.{SpectatorCommander, SpectatorState}
import gbge.ui.eps.tm.{TMCommander, TimeMachineState}
import org.scalajs.dom.html.Div

import scala.scalajs.js.annotation.JSExport

class EntryPoint {

  @JSExport
  def spectatorEntryPoint(div: Div): Unit = {
    val state = SpectatorState()
    val commander = new SpectatorCommander(div, state)
  }

  @JSExport
  def playerEntryPoint(div: Div): Unit = {
    val state = StandardStateWrapper()
    val commander = new Commander(div, state)
  }

  @JSExport
  def timeMachineEntryPoint(div: Div): Unit = {
    val state = TimeMachineState()
    val commander = TMCommander(div, state)
  }

  @JSExport
  def portalEntryPoint(div: Div): Unit = {
    val state = PortalState()
    val commander = PortalCommander(div, state)
  }

}
