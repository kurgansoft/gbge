package gbge.backend.services

import gbge.backend.Failure
import gbge.shared.JoinResponse
import gbge.shared.actions.Action
import zio.{IO, ZIO}

trait MainService {
  def joinWithName(name: String): IO[Failure, JoinResponse]
  
  def handleAction(action: Action, userId: Int): IO[Failure, Unit]
}

object MainService {
  def joinWithName(name: String): ZIO[MainService, Failure, JoinResponse] =
    ZIO.serviceWithZIO(_.joinWithName(name))
    
  def handleAction(action: Action, userId: Int): ZIO[MainService, Failure, Unit] =
    ZIO.serviceWithZIO(_.handleAction(action, userId))
}
