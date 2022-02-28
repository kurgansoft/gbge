package gbge.client

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.shared.actions.{Action, Join}
import gbge.ui.eps.player.{NewPlayerEvent, RecoverTokenEvent, RegisterWS, SetupWSConnection}
import gbge.ui.state.screenstates.ErrorInput
import gbge.ui.{TokenRecoverFactory, Urls}
import org.scalajs.dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.WebSocket
import upickle.default.{read, write}
import zio.{UIO, ZIO}

object ClientEffects {

  val recoverTokenEffect: AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
    val token = TokenRecoverFactory.getToken()
    if (token.isDefined) {
      ZIO.succeed(List(RecoverTokenEvent(token.get)))
    } else {
      ZIO.succeed(List.empty)
    }
  }

  def getPlayerWithToken(token: String): AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
    ZIO.fromFuture(_ => Ajax.get(Urls.getPlayerPostFix, headers = Map("token" -> token))).foldM(
      _ => {
        ZIO.succeed(List.empty)
      },
      success => {
        val frontendPlayer: FrontendPlayer = read[FrontendPlayer](success.response.toString)
        val frontendPlayer2 = frontendPlayer.copy(token = Some(token))
        ZIO.succeed(List(
          NewPlayerEvent(frontendPlayer2),
          SetupWSConnection
        ))
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
    ZIO.fromFuture(_ => Ajax.post(Urls.performActionPostFix, write(Join(name).serialize()))).foldM(
      {
        case AjaxException(xhr) => ZIO.succeed(List(ErrorInput(xhr.response.toString)))
        case _ => ZIO.succeed(List.empty)
      },
      success => {
        val frontendPlayer: FrontendPlayer = read[FrontendPlayer](success.response.toString)
        val token = frontendPlayer.token.get
        TokenRecoverFactory.saveToken(token)
        ZIO.succeed(List(
          NewPlayerEvent(frontendPlayer),
          SetupWSConnection
        ))
      }
    )
  }

  def submitRestActionWithToken(action: Action, token: Option[String] = None): AbstractCommander[ClientEvent] => UIO[List[ClientEvent]] = _ => {
    if (token.isDefined) {
      ZIO.fromFuture(_ => Ajax.post(Urls.performActionPostFix, write(action.serialize()), headers = Map("token" -> token.get))).foldM(
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