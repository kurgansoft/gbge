package gbge.backend.gameroutes

import cask.Response
import cask.util.Logger
import gbge.backend.MainController
import gbge.shared.FrontendUniverse
import gbge.shared.tm.{PlayerPerspective, PortalCoordinates, PortalId, SpectatorPerspective}
import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.{WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange
import sourcecode.{File, Line, Text}
import upickle.default.write

import scala.util.Try

case class TMRoutes(controller: MainController) extends cask.main.Routes {
  implicit def log: Logger = new Logger {
    override def exception(t: Throwable): Unit = println("Exception in TMRoutes: " + t)

    override def debug(t: Text[Any])(implicit f: File, line: Line): Unit = println("Debug statement in TMRoutes: " +  t)
  }

  @cask.post("/api/portalCoordinates")
  def modifyPortalCoordinates(request: cask.Request): Response[String] = this.synchronized {
    val toRead = request.readAllBytes()
    val pc = Try(upickle.default.read[PortalCoordinates](toRead))
    if (pc.isSuccess) {
      controller.modifyPortalCoordinates(pc.get)
      cask.Response("")
    } else {
      cask.Response("", 400)
    }
  }

  @cask.post("/api/save")
  def save() = {
    val success = controller.save()
    if (success)
      cask.Response("TimeMachine saved.")
    else {
      cask.Response("TimeMachine is disabled.", 403)
    }
  }

  @cask.websocket("/api/portalSocket/:portalId")
  def portalSocket(portalId: Int): cask.WebsocketResult = {
    if (MainController.timeMachineEnabled()) {
      new WebSocketConnectionCallback {
        override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
          if (controller.portals.exists(_.id == portalId)) {
            val recycledPortalId = controller.addControllerToAnExistingPortal(channel, portalId)
            val message = upickle.default.write(PortalId(recycledPortalId))
            WebSockets.sendTextBlocking(message, channel)
          } else {
            val pi: Option[Int] = if (portalId <= 0) None else Some(portalId)
            val newPortalId = controller.createNewPortal(channel, pi)
            val message = upickle.default.write(PortalId(newPortalId))
            WebSockets.sendTextBlocking(message, channel)
          }
        }
      }
    } else {
      cask.Response("TimeMachine is disabled.", 401)
    }
  }

  @cask.websocket("/api/portalSocketForClients/:portalId")
  def portalSocketForClients(portalId: Int): cask.WebsocketResult = {
    if (MainController.timeMachineEnabled()) {
      new WebSocketConnectionCallback {
        override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
          controller.joinToAnExistingPortal(channel, portalId)
          val portal = controller.portals.find(_.id == portalId)
          if (portal.isDefined && portal.get.coordinates.isDefined) {
            controller.notifyPortal(portal.get)
          }
        }
      }
    } else {
      cask.Response("TimeMachine is disabled.", 401)
    }
  }

  @cask.post("/api/resetTM/:number")
  def resetTM(number: Int): Response[String] = {
    if (MainController.timeMachineEnabled()) {
      if (number < 0 || controller.timeMachine.actions.size <= number) {
        Response("Invalid action number.", 400)
      } else {
        controller.timeMachine = controller.timeMachine.take(number)
        controller.universe = controller.timeMachine.latestUniverse._1
        controller.notifyClientsViaWebSocket()
        Response("")
      }
    } else {
      cask.Response("TimeMachine is disabled.", 401)
    }
  }

  @cask.get("api/tm/clientTM")
  def getClientTimeMachine(request: cask.Request): Response[String] = {
    if (MainController.timeMachineEnabled()) {
      val clientTimeMachine = controller.timeMachine.convertToClientTimeMachine()
      cask.Response(write(clientTimeMachine.serialize()))
    } else {
      cask.Response("TimeMachine is disabled.", 401)
    }
  }

  @cask.get("api/tm/state/:number/:playerId")
  def getTMState(number: Int, playerId: Int): Response[String] = {
    if (MainController.timeMachineEnabled()) {
      val perspective = if (playerId > 0) PlayerPerspective(playerId) else SpectatorPerspective
      val tstate: Either[String, (FrontendUniverse, Boolean)] = controller.timeMachine.getFrontendUniverseForPerspective(number, perspective)
      if (tstate.isRight)
        cask.Response(tstate.getOrElse(null)._1.serialize())
      else
        cask.Response(tstate.left.getOrElse(null), statusCode = 404)
    } else {
      cask.Response("TimeMachine is disabled.", 401)
    }
  }

  initialize()
}
