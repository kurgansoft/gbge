package gbge.backend.gameroutes

import cask.Response
import cask.endpoints.staticResources
import cask.util.Logger
import gbge.backend.{ExecuteAsyncEffect, ExecuteEffect, FAIL, GeneralFailure, MainController, OK, OKWithMessage, OKWithPlayerPayload, UnauthorizedFailure}
import gbge.shared.FrontendPlayer
import gbge.shared.actions.Action
import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.spi.WebSocketHttpExchange
import sourcecode.{File, Line, Text}
import upickle.default.{read, write}

case class GameRoutes(controller: MainController) extends cask.main.Routes {
  implicit def log: Logger = new Logger {
    override def exception(t: Throwable): Unit = println("Exception in GameRoutes: " + t)

    override def debug(t: Text[Any])(implicit f: File, line: Line): Unit = println("Debug statement in GameRoutes: " +  t)
  }

  @cask.get("/")
  def index() = {
    cask.Redirect("/static/index.html")
  }

  @cask.get("/s")
  def spectator() = {
    cask.Redirect("/static/spectator.html")
  }

  @cask.get("/tm")
  def timeMachineRoute() = {
    cask.Redirect("/static/timemachine.html")
  }

  @cask.staticResources("/static/", classOf[staticResources].getClassLoader, Seq("Content-Type" -> "text/html; charset=UTF-8"))
  def staticResourceRoutes() = "gbge/ui"

  @cask.post("/api/reset")
  def reset(request: cask.Request): Response[String] = {
    controller.reset()
    controller.notifyClientsViaWebSocket()
    cask.Response("")
  }

  @cask.get("/api/player")
  def getPlayerWithToken(request: cask.Request): Response[String] = {
    val playerToken: Option[String] = request.headers.get("token").map(_.head)
    val player = playerToken.flatMap(controller.universe.getPlayerWithToken(_))
    if (player.isEmpty)
      cask.Response("", statusCode = 404)
    else
      cask.Response(write(player.get.toFrontendPlayer()))
  }

  @cask.post("/api/performAction")
  def performAction(request: cask.Request): Response[String] = this.synchronized {
    val playerToken: Option[String] = request.headers.get("token").map(_.head)
    val toRead = request.readAllBytes()
    val action: Action = controller.parseActionWithUniverse(read[String](toRead), controller.universe)

    if (action.systemOnly) {
      cask.Response("This action is system only.", statusCode = 403)
    } else {
      val (_, result) = controller.performAction(action, playerToken)
      result match {
        case ExecuteEffect(_) => {
          cask.Response("this should never happen", statusCode = 500)
        }
        case ExecuteAsyncEffect(_) => {
          cask.Response("this should never happen either...", statusCode = 500)
        }
        case OK => {
          cask.Response("")
        }
        case OKWithPlayerPayload(player) => {
          cask.Response(write[FrontendPlayer](player))
        }
        case OKWithMessage(message) => cask.Response(message)

        case FAIL => cask.Response("", statusCode = 400)
        case GeneralFailure(message) => cask.Response(message, statusCode = 406)
        case UnauthorizedFailure(message) => cask.Response(message, statusCode = 401)
      }
    }
  }

  @cask.get("/api/universe")
  def getUniverse(): Response[String] = {
    cask.Response(controller.universe.getFrontendUniverseForPlayer().serialize())
  }

  @cask.websocket("/api/stateSocket")
  def stateSocket(): cask.WebsocketResult = {
    new WebSocketConnectionCallback {
      override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
        controller.setupStateSocketConnection(channel)
      }
    }
  }

  initialize()

}
