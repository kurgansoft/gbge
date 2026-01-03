package gbge.ui

import gbge.ui.eps.player.{CheckForTokenEvent, ClientState, CreateSSEStream}
import gbge.ui.eps.spectator.SpectatorState
import gbge.ui.eps.tm.{TMClientEvent, TimeMachineState}
import gbge.ui.token.{TokenService, URLBasedTokenService}
import org.scalajs.dom.html.Div
import uiglue.EventLoop.EventHandler
import uiglue.{Event, EventLoop, UIState}
import zio.internal.stacktracer.Tracer
import zio.{Unsafe, ZEnvironment, ZIO}

import scala.concurrent.Future
import scala.scalajs.js.annotation.JSExport

trait EntryPoint {

  implicit val ec: scala.concurrent.ExecutionContext = org.scalajs.macrotaskexecutor.MacrotaskExecutor

  implicit val tracer: Tracer = Tracer.instance
  implicit val unsafe: Unsafe = Unsafe.unsafe(x => x)

  val tokenService: ZEnvironment[TokenService] = ZEnvironment(URLBasedTokenService)

  @JSExport
  def spectatorEntryPoint(div: Div): Unit = {
    val state = SpectatorState()

    val renderFunction: (UIState[Event, Any], EventHandler[Event]) => Unit =
      (state, eventHandler) => {
        val s = state.asInstanceOf[SpectatorState]
        gbge.ui.display.Displayer
          .spectatorRootComponent(s, eventHandler)
          .renderIntoDOM(div)
      }

    val loop = EventLoop.createLoop(state, renderFunction, List(CreateSSEStream))

    Future {
      zio.Runtime.default.unsafe.run(ZIO.log("spectator entry point invoked") *> loop)
    }
  }

  @JSExport
  def playerEntryPoint(div: Div): Unit = {
    val state = ClientState()

    val renderFunction: (UIState[Event, TokenService], EventHandler[Event]) => Unit =
      (state, eventHandler) => {
        val s = state.asInstanceOf[ClientState]
        gbge.ui.display.Displayer
          .rootComponent(s, eventHandler)
          .renderIntoDOM(div)
      }

    val loop = EventLoop.createLoop(state, renderFunction, List(CheckForTokenEvent))
      .provideEnvironment(tokenService)
    
    Future {
      zio.Runtime.default.unsafe.run(ZIO.log("player entry point invoked") *> loop)
    }

  }

  @JSExport
  def timeMachineEntryPoint(div: Div): Unit = {
    val state = TimeMachineState()
    val renderFunction: (UIState[TMClientEvent, Any], EventHandler[TMClientEvent]) => Unit =
      (state, eventHandler) => {
        val s = state.asInstanceOf[TimeMachineState]
        gbge.ui.eps.tm.Displayer
          .tmComponent(s, eventHandler)
          .renderIntoDOM(div)
      }

    val loop = EventLoop.createLoop(state, renderFunction, List(gbge.ui.eps.tm.Start))

    Future {
      zio.Runtime.default.unsafe.run(ZIO.log("time machine entry point invoked") *> loop)
    }
  }
//
//  @JSExport
//  def portalEntryPoint(div: Div): Unit = {
//    val state = PortalState()
//    val renderFunction: (UIState[PortalClientEvent], EventHandler[PortalClientEvent]) => Unit =
//      (state, eventHandler) => {
//        val s = state.asInstanceOf[PortalState]
//        gbge.ui.eps.portal.Displayer
//          .portalComponent(s, eventHandler)
//          .renderIntoDOM(div)
//      }
//
//    val loop = EventLoop.createLoop(state, renderFunction, List(gbge.ui.eps.portal.Start))
//
//    Future {
//      zio.Runtime.default.unsafe.run(ZIO.log("portal entry point invoked") *> loop)
//    }
//  }

}
