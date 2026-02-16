package gbge.ui.eps.tm

import gbge.shared.FrontendUniverse
import gbge.shared.tm.{Perspective, PlayerPerspective, PortalCoordinates, SpectatorPerspective}
import gbge.ui.Urls
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client4.impl.zio.FetchZioBackend
import sttp.client4.{Request, UriContext, WebSocketStreamBackend, basicRequest}
import sttp.tapir.DecodeResult
import sttp.tapir.client.sttp4.SttpClientInterpreter
import uiglue.EventLoop.EventHandler
import zio.ZIO.*
import zio.json.JsonDecoder
import zio.json.ast.Json
import zio.{ZIO, *}

object TMEffects {

  private val backend: WebSocketStreamBackend[Task, ZioStreams] = FetchZioBackend()

  private val sttpClientInterpreter = SttpClientInterpreter()

  private lazy val actionCodecs = gbge.ui.RG.registeredGames.map(_.actionCodec)

  val retrieveActionStack: UIO[List[TMClientEvent]] = {
    val request = sttpClientInterpreter.toRequest(gbge.client.endpoints.TimeMachineEndpoints.actionHistory, None).apply(())
    for {
      _ <- ZIO.log("retrieving client TM")
      result <- backend.send(request).map(r => r.body match {
        case failure: DecodeResult.Failure =>
          println("failure was: " + failure)
          List(TimeMachineRetrievalFailed)
        case DecodeResult.Value(actionStackInTransit) =>
          val actionsAndInvokers = actionStackInTransit.getOrElse(???).toActionEntries()(actionCodecs)
          println("the content is (time machine actions):\n\t" + actionsAndInvokers.map(_.action).mkString("\n\t"))
          List(ActionsHaveArrived(actionsAndInvokers))
      })
        .tapError(e => ZIO.log("error during [retrieveTimeMachine]: " + e))
        .tapDefect(d => ZIO.log("defect during [retrieveTimeMachine]: " + d))
        .orDie
    } yield result
  }

  def retrieveTMState(actionNumber: Int, perspective: Perspective): UIO[List[TMClientEvent]] = {
    val request = perspective match {
      case SpectatorPerspective => sttpClientInterpreter.toRequest(gbge.client.endpoints.TimeMachineEndpoints.getTmSpectatorStateAtTime, None).apply(actionNumber)
      case PlayerPerspective(playerId) => sttpClientInterpreter.toRequest(gbge.client.endpoints.TimeMachineEndpoints.getTmStateAtTimeForPlayer, None).apply(actionNumber, playerId)
    }

    ZIO.log(s"retrieving state; actionNumber: [$actionNumber], perspective: [$perspective] ==> ") *> backend.send(request).map(response => response.body match
      case failure: DecodeResult.Failure =>
        println("failure was: " + failure)
        List.empty
      case DecodeResult.Value(Left(_)) =>
        println("different error")
        List.empty
      case DecodeResult.Value(Right(stateEncodedAsString)) =>
        JsonDecoder[Json].decodeJson(stateEncodedAsString) match {
          case Right(json) =>
            val fu: FrontendUniverse = FrontendUniverse.decode(json)
            println(s"the decoded fu:\n$fu")
            List(TMStateArrived(actionNumber, perspective, fu))
          case _ =>
            println("wrong data")
            List.empty
        }


    ).orDie
  }

  def createOrReusePortal(portalId: Option[Int] = None): EventHandler[TMClientEvent] => UIO[List[TMClientEvent]] = eventHandler => {

//    val url = if (portalId.isDefined) {
//      Urls.portalSocketURL + "/" + portalId.get
//    } else {
//      Urls.portalSocketURL + "/0"
//    }
//    val portalSocket = new WebSocket(url)
//    portalSocket.onmessage = message => {
//      val id: Try[TMMessage] = ??? //Try(upickle.default.read[TMMessage](message.data.toString))
//      if (id.isSuccess) {
//        import gbge.shared.tm.PortalId
//        id.get match {
//          case PortalId(id) =>
//            eventHandler(TMMessageContainer(PortalId(id)))
//          case _ =>
//        }
//      }
//    }
    ZIO.log(s"createOrReusePortal function; potralId: [$portalId] (...not doing anything...)").as(List.empty)
  }

  def resetTmToNumber(number: Int): UIO[List[TMClientEvent]] = {
    val request = sttpClientInterpreter.toRequest(gbge.client.endpoints.TimeMachineEndpoints.reset, None).apply(number)
    for {
      result <- backend.send(request).map(response => response.body match
        case failure: DecodeResult.Failure =>
          println("failure was: " + failure)
          List.empty[TMClientEvent]
        case DecodeResult.Value(Left(_)) =>
          println("different error")
          List.empty[TMClientEvent]
        case DecodeResult.Value(Right(_)) =>
         List(TmGotShrunk(number))
      ).orDie
    } yield result
  }

  val save: UIO[List[TMClientEvent]] = {
    val request: Request[DecodeResult[Either[Nothing, Unit]]] = sttpClientInterpreter.toRequest(gbge.client.endpoints.TimeMachineEndpoints.save, None)(())
    for {
      _ <- backend.send(request).orDie
    } yield List.empty
  }

  private def persistToHash(string: String): UIO[List[TMClientEvent]] = {
    org.scalajs.dom.window.location.hash = string
    ZIO.succeed(List.empty)
  }

  private def submitPortalCoordinates(coordinates: PortalCoordinates): UIO[List[TMClientEvent]] = {
    val request = basicRequest.post(uri"${Urls.setPortalCoordinatesPostFix}").body("write(coordinates)")
    backend.send(request).as(List.empty).orDie
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
