package gbge.ui.eps.tm

import gbge.client.TextDecoder
import gbge.shared.{ClientTimeMachine, FrontendUniverse}
import gbge.shared.tm.{Perspective, PortalCoordinates, TMMessage}
import gbge.ui.Urls
import org.scalajs.dom.{HttpMethod, RequestInit, WebSocket}
import uiglue.EventLoop.EventHandler
import upickle.default.write
import zio.{UIO, ZIO}

import scala.util.Try

object TMEffects {

  val textDecoder = new TextDecoder()

  val retrieveTimeMachine: UIO[List[TMClientEvent]] = {
    ZIO.fromPromiseJS(org.scalajs.dom.fetch(Urls.tmActionsPostFix)).foldM(
      _ => ZIO.succeed(List(
        TimeMachineRetrievalFailed)
      ),
      successValue => {
        ZIO.fromPromiseJS(successValue.arrayBuffer()).map(textDecoder.decode)
          .flatMap(d => {
            val tmString = upickle.default.read[String](d)
            val clientTM: ClientTimeMachine = ClientTimeMachine.decode(tmString)
            ZIO.succeed(List(
              TimeMachineHaveArrived(clientTM)
            ))
          }).orDie
      }
    )
  }

  def retrieveTMState(actionNumber: Int, perspective: Perspective): UIO[List[TMClientEvent]] = {
    ZIO.fromPromiseJS(org.scalajs.dom.fetch(Urls.tmStatePostFix + actionNumber + "/" + perspective.id)).foldM(
      _ => {
        ZIO.succeed(List.empty)
      },
      success => {
        ZIO.fromPromiseJS(success.arrayBuffer()).map(textDecoder.decode)
          .flatMap(d => {
            val fu = FrontendUniverse.decode(d)
            ZIO.succeed(List(
              TMStateArrived(actionNumber, perspective, fu)
            ))
          }).orDie
      }
    )
  }

  def createOrReusePortal(portalId: Option[Int] = None): EventHandler[TMClientEvent] => UIO[List[TMClientEvent]] = eventHandler => {
    ZIO.effectTotal {
      val url = if (portalId.isDefined) {
        Urls.portalSocketURL + "/" + portalId.get
      } else {
        Urls.portalSocketURL + "/0"
      }
      val portalSocket = new WebSocket(url)
      portalSocket.onmessage = message => {
        val id = Try(upickle.default.read[TMMessage](message.data.toString))
        if (id.isSuccess) {
          import gbge.shared.tm.PortalId
          id.get match {
            case PortalId(id) =>
              eventHandler(TMMessageContainer(PortalId(id)))
            case _ =>
          }
        }
      }
      List.empty
    }
  }

  def resetTmToNumber(number: Int): UIO[List[TMClientEvent]] = {
    ZIO.fromPromiseJS(org.scalajs.dom.fetch(Urls.resetTMPostFix + number, new RequestInit {
      method = HttpMethod.POST
    })).foldM(
      _ => ZIO.succeed(List.empty),
      _ => ZIO.succeed(List(TmGotShrunk(number)))
    )
  }

  val save: UIO[List[TMClientEvent]] = {
    ZIO.fromPromiseJS(org.scalajs.dom.fetch(Urls.savePostFix, new RequestInit {
      method = HttpMethod.POST
    })).foldM(
      _ => ZIO.succeed(List.empty),
      _ => ZIO.succeed(List.empty)
    )
  }

  def persistToHash(string: String): UIO[List[TMClientEvent]] = {
    ZIO.effectTotal {
      org.scalajs.dom.window.location.hash = string
      List.empty
    }
  }

  def submitPortalCoordinates(coordinates: PortalCoordinates): UIO[List[TMClientEvent]] = {
    ZIO.fromPromiseJS(org.scalajs.dom.fetch(Urls.setPortalCoordinatesPostFix, new RequestInit {
     body = write(coordinates)
     method = HttpMethod.POST
    })).foldM(
      _ => ZIO.succeed(List.empty),
      _ => ZIO.succeed(List.empty)
    )
  }

  def persistToHashAndSubmitPortalCoordinates(tmState: TimeMachineState): UIO[List[TMClientEvent]] = {
    if (tmState.portalId.isEmpty) {
      persistToHash(tmState.stringToPersist)
    } else {
      val first = persistToHash(tmState.stringToPersist)
      val second = submitPortalCoordinates(tmState.getPortalCoordinates)
      second *> first
    }
  }

  val recoverFromHash: UIO[List[TMClientEvent]] = {
    ZIO.effectTotal {
      val theHash = org.scalajs.dom.window.location.hash
      List(RecoveredHash(theHash))
    }
  }

  def justReturnAnEvent(event: TMClientEvent): UIO[List[TMClientEvent]] = {
    ZIO.succeed(List(event))
  }

}
