package gbge.client

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.shared.actions.{Action, Join}
import gbge.ui.eps.player.{NewPlayerEvent, RecoverTokenEvent, RegisterWS, SetupWSConnection}
import gbge.ui.state.screenstates.ErrorInput
import gbge.ui.{TokenRecoverFactory, Urls}
import org.scalajs.dom.{Headers, HttpMethod, RequestInit, WebSocket}
import upickle.default.{read, write}
import zio.{UIO, ZIO}

object ClientEffects {

  val textDecoder = new TextDecoder()

  val recoverTokenEffect: AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
    val token = TokenRecoverFactory.getToken()
    if (token.isDefined) {
      ZIO.succeed(List(RecoverTokenEvent(token.get)))
    } else {
      ZIO.succeed(List.empty)
    }
  }

  def getPlayerWithToken(token: String): AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
    import scala.scalajs._
    val fetchPromise = org.scalajs.dom.fetch(Urls.getPlayerPostFix, new RequestInit {
      method = HttpMethod.GET
      headers = new Headers(js.Array(
        js.Array("token", token)
      ))
    })
    ZIO.fromPromiseJS(fetchPromise).foldM(
      _ => {
        ZIO.succeed(List.empty)
      },
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

  def createWebSocketConnection(token: Option[String]): AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = commander  => {
    val stateSocket = new WebSocket(Urls.stateSocketURL)
    stateSocket.onmessage = message => {
      val newUniverse: FrontendUniverse = FrontendUniverse.decode(message.data.toString)
      commander.addAnEventToTheEventQueue(NewFU(newUniverse))
    }
    stateSocket.onclose = _ => {
      commander.addAnEventToTheEventQueue(WebsocketConnectionBrokeDown)
    }
    if (token.isDefined)
      stateSocket.onopen = _ => stateSocket.send(token.get)
    else
      stateSocket.onopen = _ => stateSocket.send("spectator")
    ZIO.succeed(List(
      RegisterWS(stateSocket)
    ))
  }

  def joinWithName(name: String): AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
    val fetchPromise = org.scalajs.dom.fetch(Urls.performActionPostFix, new RequestInit {
      method = HttpMethod.POST
      body = write(Join(name).serialize())
    })
    ZIO.fromPromiseJS(fetchPromise).foldM(
      failure => {
        ZIO.succeed(List(ErrorInput(failure.getMessage)))
      },
      success => {
        val zp = ZIO.fromPromiseJS(success.arrayBuffer()).map(textDecoder.decode)

        zp.flatMap(decodedPayload => {
          val frontendPlayer: FrontendPlayer = read[FrontendPlayer](decodedPayload)
          val token = frontendPlayer.token.get
          TokenRecoverFactory.saveToken(token)
          ZIO.succeed(List[ClientEvent](
            NewPlayerEvent(frontendPlayer),
            SetupWSConnection
          ))
        }).orDie
      }
    )
  }

  def submitRestActionWithToken(action: Action, token: Option[String] = None): AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
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
        _ => {
          ZIO.succeed(List.empty)
        },
        _ => {
          ZIO.succeed(List.empty)
        }
      )
    } else {
      ZIO.succeed(List.empty)
    }
  }
}