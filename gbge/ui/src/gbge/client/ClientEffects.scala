package gbge.client

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.shared.actions.{Action, Join}
import gbge.ui.eps.player.{NewPlayerEvent, PlayerEvent, RecoverTokenEvent, RegisterWS, SetupWSConnection}
import gbge.ui.state.screenstates.ErrorInput
import gbge.ui.{TokenRecoverFactory, Urls}
import org.scalajs.dom.{Headers, HttpMethod, RequestInit, WebSocket}
import uiglue.EventLoop
import upickle.default.{read, write}
import zio.{UIO, ZIO}

object ClientEffects {

  val textDecoder = new TextDecoder()

  val recoverTokenEffect: UIO[List[PlayerEvent]] = {
    val token = TokenRecoverFactory.getToken()
    if (token.isDefined) {
      ZIO.succeed(List(RecoverTokenEvent(token.get)))
    } else {
      ZIO.succeed(List.empty)
    }
  }

  def getPlayerWithToken(token: String): UIO[List[PlayerEvent]] = {
    import scala.scalajs._
    val fetchPromise = org.scalajs.dom.fetch(Urls.getPlayerPostFix, new RequestInit {
      method = HttpMethod.GET
      headers = new Headers(js.Array(
        js.Array("token", token)
      ))
    })
    ZIO.fromPromiseJS(fetchPromise).foldM(
      _ => ZIO.succeed(List.empty),
      success => {
        val zp = ZIO.fromPromiseJS(success.arrayBuffer()).map(textDecoder.decode)
        zp.flatMap(decodedPlayer => {
          val frontendPlayer: FrontendPlayer = read[FrontendPlayer](decodedPlayer)
          val frontendPlayer2 = frontendPlayer.copy(token = Some(token))
          ZIO.succeed(List(
            NewPlayerEvent(frontendPlayer2),
            SetupWSConnection
          ))
        }).orDie
      }
    )
  }

  def createWebSocketConnection(token: Option[String], eventHandler: EventLoop.EventHandler[GeneralEvent]): UIO[List[RegisterWS]] = {
    val stateSocket = new WebSocket(Urls.stateSocketURL)
    stateSocket.onmessage = message => {
      val newUniverse: FrontendUniverse = FrontendUniverse.decode(message.data.toString)
      eventHandler(NewFU(newUniverse))
    }
    stateSocket.onclose = _ => {
      eventHandler(WebsocketConnectionBrokeDown)
    }
    if (token.isDefined)
      stateSocket.onopen = _ => stateSocket.send(token.get)
    else
      stateSocket.onopen = _ => stateSocket.send("spectator")
    ZIO.succeed(List(
      RegisterWS(stateSocket)
    ))
  }

  def joinWithName(name: String): UIO[List[PlayerEvent]] = {
    val fetchPromise = org.scalajs.dom.fetch(Urls.performActionPostFix, new RequestInit {
      method = HttpMethod.POST
      body = write(Join(name).serialize())
    })
    ZIO.fromPromiseJS(fetchPromise).foldM(
      _ => ZIO.succeed(List.empty),
      success => {
        val zp = ZIO.fromPromiseJS(success.arrayBuffer()).map(textDecoder.decode)
        zp.flatMap(decodedPayload => {
          if (success.ok) {
            val frontendPlayer: FrontendPlayer = read[FrontendPlayer](decodedPayload)
            val token = frontendPlayer.token.get
            TokenRecoverFactory.saveToken(token)
            ZIO.succeed(List(
              NewPlayerEvent(frontendPlayer),
              SetupWSConnection
            ))
          } else {
            ZIO.succeed(List(ErrorInput(decodedPayload)))
          }
        }).orDie
      }
    )
  }

  def submitRestActionWithToken(action: Action, token: Option[String] = None): UIO[List[Nothing]] = {
    if (token.isDefined) {
      import scala.scalajs._
      val fetchPromise = org.scalajs.dom.fetch(Urls.performActionPostFix, new RequestInit {
        method = HttpMethod.POST
        body = write(action.serialize())
        headers = new Headers(js.Array(
          js.Array("token", token.get)
        ))
      })

      ZIO.fromPromiseJS(fetchPromise).foldM(
        _ => ZIO.succeed(List.empty),
        _ => ZIO.succeed(List.empty)
      )
    } else {
      ZIO.succeed(List.empty)
    }
  }
}