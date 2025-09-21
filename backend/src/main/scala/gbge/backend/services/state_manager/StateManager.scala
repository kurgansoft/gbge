package gbge.backend.services.state_manager

import gbge.backend.Failure
import gbge.backend.models.Universe
import gbge.backend.services.TokenGenerator
import gbge.shared.actions.Action
import zio.stream.ZStream
import zio.{IO, ZIO}

trait StateManager {
  def update(action: Action, playerId: Option[Int] = None): IO[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])]
  
  val universeStream: ZStream[Any, Nothing, Universe]
}

object StateManager {
  def update(action: Action, playerId: Option[Int] = None): ZIO[StateManager, Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])] =
    ZIO.serviceWithZIO(_.update(action, playerId))
}
