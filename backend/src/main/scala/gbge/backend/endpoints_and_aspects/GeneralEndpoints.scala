package gbge.backend.endpoints_and_aspects

import gbge.backend.BackendGameProps
import gbge.shared.JoinResponse
import gbge.shared.actions.GeneralAction
import zio.ZNothing
import zio.http.*
import zio.http.codec.HttpCodec.content
import zio.http.codec.{HttpCodec, HttpContentCodec, PathCodec, StatusCodec}
import zio.http.endpoint.{AuthType, Endpoint}
import zio.schema.Schema

object GeneralEndpoints {
  
  val sse =
    Endpoint(RoutePattern.GET / "publicEvents")
      .inCodec(HttpCodec.header(Header.Accept).const(Header.Accept(MediaType.text.`event-stream`)))
      .outStream[ServerSentEvent[String]](MediaType.text.`event-stream`)

  val sseWithAuthentication =
    Endpoint(RoutePattern.GET / "events")
      .auth(AuthType.Bearer)
      .inCodec(HttpCodec.header(Header.Accept).const(Header.Accept(MediaType.text.`event-stream`)))
      .outStream[ServerSentEvent[String]](MediaType.text.`event-stream`)

  val joinEndpoint = Endpoint(RoutePattern.POST / "join")
    .inCodec(HttpCodec.content[String](MediaType.text.plain))
    .outCodec(StatusCodec.Ok ++ HttpCodec.content[JoinResponse](MediaType.application.json))
    .outError[String](Status.BadRequest)
  
  val getFUEndpoint = Endpoint(RoutePattern.GET / "fu")
    .outCodec(HttpCodec.content[String](MediaType.application.json))

  val getPlayerEndpoint = Endpoint(RoutePattern.GET / "player")
    .outCodec(HttpCodec.content[Int](MediaType.application.json))

  val performGeneralActionEndpoint: Endpoint[Unit, GeneralAction, ZNothing, Unit, AuthType.Bearer] =
    Endpoint(RoutePattern.POST / "performAction")
      .auth(AuthType.Bearer)
      .inCodec(content[GeneralAction])
      .outCodec(StatusCodec.Ok)

  def generateGameSpecificActionEndpoint(game: BackendGameProps[_,_]) = {
    Endpoint(RoutePattern.POST / "performAction" / game.urlFragment)
      .auth(AuthType.Bearer)
      .inCodec(game.contentCodec)
      .outCodec(StatusCodec.Ok)
  }
}
