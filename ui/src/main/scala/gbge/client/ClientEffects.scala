package gbge.client

import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.shared.actions.{Action, Join}
import gbge.ui.eps.player.{NewPlayerEvent, PlayerEvent, RecoverTokenEvent, RegisterWS, SetupWSConnection}
import gbge.ui.{TokenRecoverFactory, Urls}
import org.scalajs.dom.{Headers, HttpMethod, RequestInit, WebSocket}
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3.{SttpBackend, UriContext, basicRequest}
import sttp.model.Header
import uiglue.EventLoop
import upickle.default.{read, write}
import zio.{Task, UIO, ZIO}

object ClientEffects {

  private val backend: SttpBackend[Task, ZioStreams with capabilities.WebSockets] = FetchZioBackend()

  val recoverTokenEffect: UIO[List[PlayerEvent]] = {
    val token = TokenRecoverFactory.getToken()
    if (token.isDefined) {
      ZIO.succeed(List(RecoverTokenEvent(token.get)))
    } else {
      ZIO.succeed(List.empty)
    }
  }

  def getPlayerWithToken(token0: String): UIO[List[PlayerEvent]] = {
    import scala.scalajs._
    val fetchPromise = org.scalajs.dom.fetch(Urls.getPlayerPostFix, new RequestInit {
      method = HttpMethod.GET
      headers = new Headers(js.Array(
        js.Array("token", token0)
      ))
    })

    val request = basicRequest.get(uri"${Urls.getPlayerPostFix}").headers(Header("token", token0))
    
    backend.send(request).map(response => response.body match
      case Left(_) => List.empty
      case Right(decodedPlayer) =>
        val frontendPlayer: FrontendPlayer = read[FrontendPlayer](decodedPlayer)
        val frontendPlayer2 = frontendPlayer.copy(token = Some(token0))
        List(
          NewPlayerEvent(frontendPlayer2),
          SetupWSConnection
        )
    ).orDie 
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

    val request = basicRequest.post(uri"${Urls.performActionPostFix}").body(write(Join(name).serialize()))
    
    backend.send(request).flatMap(response => response.body match
      case Left(_) => ZIO.succeed(List.empty)
      case Right(decodedPayload) => 
        val frontendPlayer: FrontendPlayer = read[FrontendPlayer](decodedPayload)
        val token = frontendPlayer.token.get
        TokenRecoverFactory.saveToken(token)
        ZIO.succeed(List(
          NewPlayerEvent(frontendPlayer),
          SetupWSConnection
        ))
    ).orDie
  }

  def submitRestActionWithToken(action: Action, token: Option[String] = None): UIO[List[Nothing]] = {
    if (token.isDefined) {
      val request = basicRequest.post(uri"${Urls.performActionPostFix}").body(write(action.serialize())) 
        .headers(Header("token", token.get))
      backend.send(request).orDie *> ZIO.succeed(List.empty)
    } else {
      ZIO.succeed(List.empty)
    }
  }
}