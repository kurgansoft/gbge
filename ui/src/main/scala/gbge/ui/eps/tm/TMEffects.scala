package gbge.ui.eps.tm

import gbge.shared.{ClientTimeMachine, FrontendUniverse}
import gbge.shared.tm.{Perspective, PortalCoordinates, TMMessage}
import gbge.ui.Urls
import org.scalajs.dom.WebSocket
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3.{SttpBackend, UriContext, basicRequest}
import uiglue.EventLoop.EventHandler
import upickle.default.{read, write}
import zio.{ZIO, *}
import zio.ZIO.*

import scala.util.Try

object TMEffects {

  private val backend: SttpBackend[Task, ZioStreams with capabilities.WebSockets] = FetchZioBackend()

  val retrieveTimeMachine: UIO[List[TMClientEvent]] = {
    val request = basicRequest.get(uri"${Urls.tmActionsPostFix}")
    ZIO.log("retrieving client TM") *> backend.send(request).map(response => response.body match
      case Left(_) => List(TimeMachineRetrievalFailed)
      case Right(content) =>
        val clientTM: ClientTimeMachine = ClientTimeMachine.decode(content)
        List(
          TimeMachineHaveArrived(clientTM)
        )
    ).orDie
  }

  def retrieveTMState(actionNumber: Int, perspective: Perspective): UIO[List[TMClientEvent]] = {
    val request = basicRequest.get(uri"${Urls.tmStatePostFix}$actionNumber/${perspective.id}")
    ZIO.log("retrieving state") *> backend.send(request).map(response => response.body match
      case Left(_) => List.empty
      case Right(content) =>
        val fu: FrontendUniverse = FrontendUniverse.decode(content)
        List(TMStateArrived(actionNumber, perspective, fu))
    ).orDie
  }

  def createOrReusePortal(portalId: Option[Int] = None): EventHandler[TMClientEvent] => UIO[List[TMClientEvent]] = eventHandler => {
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
    ZIO.succeed(List.empty)
  }

  def resetTmToNumber(number: Int): UIO[List[TMClientEvent]] = {
    val request = basicRequest.post(uri"${Urls.resetTMPostFix}$number")
    backend.send(request).map(response => response.body match
      case Left(_) => List.empty
      case Right(_) => List(TmGotShrunk(number))
    ).orDie
  }

  val save: UIO[List[TMClientEvent]] = {
    val request = basicRequest.post(uri"${Urls.savePostFix}")
    backend.send(request).map(_ => List.empty).orDie
  }

  private def persistToHash(string: String): UIO[List[TMClientEvent]] = {
    org.scalajs.dom.window.location.hash = string
    ZIO.succeed(List.empty)
  }

  private def submitPortalCoordinates(coordinates: PortalCoordinates): UIO[List[TMClientEvent]] = {
    val request = basicRequest.post(uri"${Urls.setPortalCoordinatesPostFix}").body(write(coordinates))
    backend.send(request).map(_ => List.empty).orDie
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
    val theHash = org.scalajs.dom.window.location.hash
    ZIO.succeed(List(RecoveredHash(theHash)))
  }

  def justReturnAnEvent(event: TMClientEvent): UIO[List[TMClientEvent]] = {
    ZIO.succeed(List(event))
  }

}
