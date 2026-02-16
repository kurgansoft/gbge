package gbge.backend.endpoints_and_aspects

import gbge.shared.tm.ActionStackInTransit
import zio.ZNothing
import zio.http.{MediaType, RoutePattern, Status}
import zio.http.codec.{HttpCodec, PathCodec, StatusCodec}
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.Endpoint

object TimeMachineEndpoints {
  val actionHistory: Endpoint[Unit, Unit, ZNothing, ActionStackInTransit, None] = Endpoint(RoutePattern.GET / "tm" / "actions")
    .outCodec(StatusCodec.Ok ++ HttpCodec.content[ActionStackInTransit](MediaType.application.json))

  val reset: Endpoint[Unit, Int, Unit, Unit, None] = Endpoint(RoutePattern.POST / "tm" / "reset")
    .inCodec(HttpCodec.content[Int](MediaType.text.plain))
    .outCodec(StatusCodec.Ok)
    .outError[Unit](Status.BadRequest)

  val save: Endpoint[Unit, Unit, ZNothing, Unit, None] = Endpoint(RoutePattern.POST / "tm" / "save")
    .outCodec(StatusCodec.Ok)

  val getTmSpectatorStateAtTime: Endpoint[Int, Int, Unit, String, None] = Endpoint(RoutePattern.GET / "tm" / "getSpectatorState" / PathCodec.int("index"))
    .outCodec(HttpCodec.content[String](MediaType.application.json))
    .outError[Unit](Status.BadRequest)

  val getTmStateAtTimeForPlayer: Endpoint[(Int, Int), (Int, Int), Unit, String, None] = Endpoint(RoutePattern.GET / "tm" / "getPlayerState" / PathCodec.int("index") / PathCodec.int("playerId"))
    .outCodec(HttpCodec.content[String](MediaType.application.json))
    .outError[Unit](Status.BadRequest)
  
}