package gbge.client.events_and_effects

import gbge.client.events_and_effects.{
  ActionsHaveArrived,
  RecoveredHash,
  TMClientEvent,
  TMStateArrived,
  TimeMachineRetrievalFailed,
  TmGotShrunk
}
import gbge.shared.FrontendUniverse
import gbge.shared.tm.{
  ActionStackInTransit,
  Perspective,
  PlayerPerspective,
  PortalCoordinates,
  SpectatorPerspective
}
import gbge.ui.Urls
import gbge.ui.eps.tm.TimeMachineState
import sttp.capabilities.zio.ZioStreams
import sttp.client4.{Request, UriContext, WebSocketStreamBackend, basicRequest}
import sttp.client4.asString
import sttp.client4.impl.zio.FetchZioBackend
import uiglue.EventLoop.EventHandler
import zio.*
import zio.ZIO.*
import zio.blocks.schema.json.Json

object TMEffects {

  private val backend: WebSocketStreamBackend[Task, ZioStreams] =
    FetchZioBackend()

  private lazy val actionCodecs = gbge.ui.RG.registeredGames.map(_.actionCodec)

  val retrieveActionStack: UIO[List[TMClientEvent]] = {
    val request: Request[Either[String, String]] =
      basicRequest
        .get(uri"${Urls.tmActionsPostFix}")

    for {
      _ <- ZIO.log("retrieving client TM")
      lists <- backend
        .send(request)
        .map(response =>
          response.body match {
            case Left(message) => List(TimeMachineRetrievalFailed)
            case Right(encodedActionStackAsString) =>
              ActionStackInTransit.jsonCodec.decode(
                encodedActionStackAsString
              ) match {
                case Left(error) =>
                  List(TimeMachineRetrievalFailed)
                case Right(actionStackInTransit: ActionStackInTransit) =>
                  val actionEntries =
                    actionStackInTransit.toActionEntries()(using actionCodecs)
                  List(ActionsHaveArrived(actionEntries))
              }
          }
        )
        .tapError(e => ZIO.log("error during [retrieveTimeMachine]: " + e))
        .tapDefect(d => ZIO.log("defect during [retrieveTimeMachine]: " + d))
        .orDie
    } yield lists
  }

  def retrieveTMState(
      actionNumber: Int,
      perspective: Perspective
  ): UIO[List[TMClientEvent]] = {
    val request = perspective match {
      case SpectatorPerspective =>
        basicRequest
          .get(
            uri"${Urls.urlPrefix}tm/getSpectatorState/$actionNumber"
          )
          .response(asString)
      case PlayerPerspective(playerId) =>
        basicRequest
          .get(
            uri"${Urls.urlPrefix}tm/getPlayerState/$actionNumber/$playerId"
          )
          .response(asString)
    }

    ZIO.log(
      s"retrieving state; actionNumber: [$actionNumber], perspective: [$perspective] ==> "
    ) *> backend
      .send(request)
      .map(response =>
        response.body match
          case Left(error) =>
            List.empty[TMClientEvent]
          case Right(stateEncodedAsString) =>
            Json.jsonCodec.decode(stateEncodedAsString) match {
              case Right(json) =>
                val fu: FrontendUniverse = FrontendUniverse.decode(json)
                List(TMStateArrived(actionNumber, perspective, fu))
              case Left(error) =>
                List.empty[TMClientEvent]
            }
      )
      .orDie
  }

  def createOrReusePortal(
      portalId: Option[Int] = None
  ): EventHandler[TMClientEvent] => UIO[List[TMClientEvent]] = eventHandler => {

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
    ZIO
      .log(
        s"createOrReusePortal function; potralId: [$portalId] (...not doing anything...)"
      )
      .as(List.empty)
  }

  def resetTmToNumber(number: Int): UIO[List[TMClientEvent]] = {
    val request = basicRequest
      .post(uri"${Urls.resetTMPostFix}")
      .body(number.toString)

    backend
      .send(request)
      .map(response =>
        response.body match
          case Left(error) =>
            List.empty[TMClientEvent]
          case Right(_) =>
            List(TmGotShrunk(number))
      )
      .orDie
  }

  val save: UIO[List[TMClientEvent]] = {
    val request = basicRequest
      .post(uri"/tm/save")

    backend
      .send(request)
      .map(response =>
        response.body match
          case Left(error) =>
            List.empty[TMClientEvent]
          case Right(_) =>
            List.empty[TMClientEvent]
      )
      .orDie
  }

  private def persistToHash(string: String): UIO[List[TMClientEvent]] = {
    org.scalajs.dom.window.location.hash = string
    ZIO.succeed(List.empty)
  }

  private def submitPortalCoordinates(
      coordinates: PortalCoordinates
  ): UIO[List[TMClientEvent]] = {
    val request = basicRequest
      .post(uri"${Urls.setPortalCoordinatesPostFix}")
      .body("write(coordinates)")
    backend.send(request).as(List.empty).orDie
  }

  def persistToHashAndSubmitPortalCoordinates(
      tmState: TimeMachineState
  ): UIO[List[TMClientEvent]] = {
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
