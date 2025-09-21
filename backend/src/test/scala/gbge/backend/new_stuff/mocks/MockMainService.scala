package gbge.backend.new_stuff.mocks

import gbge.backend.Failure
import gbge.backend.services.MainService
import gbge.shared.JoinResponse
import gbge.shared.actions.Action
import zio.{IO, URLayer, ZIO, ZLayer}
import zio.mock.{Mock, Proxy}

object MockMainService extends Mock[MainService] {
  
  object JoinWithName extends Effect[String, Failure, JoinResponse]
  object HandleAction extends Effect[(Action, Int), Failure, Unit]

  val compose: URLayer[Proxy, MainService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new MainService {
        override def joinWithName(name: String): IO[Failure, JoinResponse] =
          proxy(JoinWithName, name)

        override def handleAction(action: Action, userId: Int): IO[Failure, Unit] =
          proxy(HandleAction, action, userId)
      }
    }

}
