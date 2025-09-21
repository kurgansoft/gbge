package gbge.backend.gameroutes

import gbge.backend.endpoints_and_aspects.TimeMachineEndpoints
import gbge.backend.services.state_manager.TimeMachine
import zio.ZIO
import zio.http.*

object TMRoutes {
  val resetRoute: Route[TimeMachine, Nothing] =
    TimeMachineEndpoints.tmResetEndpoint.implement(index => for {
      stateManager <- ZIO.service[TimeMachine]
      _ <- ZIO.log(s"Attempting to reset with index [$index]...")
      _ <- stateManager.reset(index)
      _ <- ZIO.log("Reset succeeded!")
    } yield ())
}
