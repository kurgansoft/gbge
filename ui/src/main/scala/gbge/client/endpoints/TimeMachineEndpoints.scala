package gbge.client.endpoints

import gbge.shared.tm.*
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.Void
import sttp.tapir.json.zio.*
import sttp.tapir.{
  PublicEndpoint,
  emptyOutput,
  endpoint,
  path,
  plainBody,
  statusCode,
  stringToPath
}
import zio.json.{JsonDecoder, JsonEncoder}

object TimeMachineEndpoints {

  import sttp.tapir.generic.auto.*

  val actionHistory: PublicEndpoint[Unit, Nothing, ActionStackInTransit, Any] =
    endpoint.in("tm" / "actions")
      .out(jsonBody[ActionStackInTransit])
      .errorOut(Void())

  val reset: PublicEndpoint[Int, Unit, Unit, Any] =
    endpoint.post
      .in("tm" / "reset")
      .in(plainBody[Int])

  val save: PublicEndpoint[Unit, Nothing, Unit, Any] =
    endpoint.post
      .in("tm" / "save")
      .errorOut(Void())

  val getTmSpectatorStateAtTime: PublicEndpoint[Int, Unit, String, Any] =
    endpoint.get
      .in("tm" / "getSpectatorState" / path[Int]("index"))
      .out(jsonBody[String])
      .errorOut(emptyOutput.and(statusCode(StatusCode.BadRequest)))

  val getTmStateAtTimeForPlayer: PublicEndpoint[(Int, Int), Unit, String,  Any] =
    endpoint.get
      .in("tm" / "getPlayerState" / path[Int]("index") / path[Int]("playerId"))
      .out(jsonBody[String])
      .errorOut(emptyOutput.and(statusCode(StatusCode.BadRequest)))


}
