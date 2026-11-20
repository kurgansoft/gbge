package gbge.backend.services

import gbge.backend.services.state_manager.StateManager
import gbge.backend.{Failure, BackendGameProps}
import gbge.shared.JoinResponse
import gbge.shared.actions.{Action, Join, ProvideToken}
import zio.{IO, ZEnvironment, ZIO, ZLayer}

case class MainServiceLive(
    tokenGenerator: TokenGenerator,
    stateManager: StateManager,
    games: Seq[BackendGameProps[?, ?, ?]],
    maxNoOfPlayers: Int = 12
) extends MainService {
  override def joinWithName(name: String): IO[Failure, JoinResponse] = for {
    pair <- stateManager
      .update(Join(name), None)
      .provideEnvironment(ZEnvironment(tokenGenerator))
    (updatedUniverse, effect) = pair
    playerId = updatedUniverse.candidate.get.id

    action <- effect.provideEnvironment(ZEnvironment(tokenGenerator))
    provideTokenAction = action.head.asInstanceOf[ProvideToken]

    pair <- stateManager
      .update(provideTokenAction, None)
      .provideEnvironment(ZEnvironment(tokenGenerator))
    (updatedUniverse, _) = pair
    token = updatedUniverse.players
      .find(pair => pair._2.id == playerId)
      .map(_._1)
      .get
  } yield JoinResponse(playerId, token)

  override def handleAction(action: Action, userId: Int): IO[Failure, Unit] =
    def process(currentAction: Action): IO[Failure, Unit] =
      for {
        pair <- stateManager.update(currentAction, Some(userId))
        (_, effect) = pair
        nextAction <- effect.provideEnvironment(ZEnvironment(tokenGenerator))
        _ <- nextAction match {
          case Some(action) => process(action)
          case None         => ZIO.unit
        }
      } yield ()

    process(action)
}

object MainServiceLive {
  val layer: ZLayer[
    TokenGenerator & StateManager & Seq[BackendGameProps[?, ?, ?]],
    Nothing,
    MainService
  ] = ZLayer {
    for {
      tokenGenerator <- ZIO.service[TokenGenerator]
      stateManager <- ZIO.service[StateManager]
      games <- ZIO.service[Seq[BackendGameProps[?, ?, ?]]]
    } yield MainServiceLive(tokenGenerator, stateManager, games)
  }
}
