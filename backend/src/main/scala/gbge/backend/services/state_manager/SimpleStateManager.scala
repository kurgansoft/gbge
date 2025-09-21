package gbge.backend.services.state_manager

import gbge.backend.{Failure, BackendGameProps}
import gbge.backend.models.Universe
import gbge.backend.services.TokenGenerator
import gbge.shared.actions.Action
import zio.{IO, Ref, ZIO, ZLayer}
import zio.stream.{SubscriptionRef, ZStream}

case class SimpleStateManager(subscriptionRef: SubscriptionRef[Universe]) extends StateManager  {
  override def update(action: Action, playerId: Option[Int]): IO[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])] = for {
    effectRef <- Ref.make[ZIO[TokenGenerator, Failure, Seq[Action]]](ZIO.succeed(Seq.empty))
    updatedUniverse <- subscriptionRef.updateAndGetZIO(u => {
      u.reduce(action, playerId) match
        case Left(failure) => ZIO.fail(failure)
        case Right((u2, effect)) => for {
          _ <- effectRef.set(effect)
        } yield u2
    })
    effect <- effectRef.get
  } yield (updatedUniverse, effect)

  override val universeStream: ZStream[Any, Nothing, Universe] = subscriptionRef.changes
}

object SimpleStateManager {
  val layer: ZLayer[Seq[BackendGameProps[_,_]], Nothing, StateManager] = ZLayer {
    for {
      games <- ZIO.service[Seq[BackendGameProps[_,_]]]
      uref <- SubscriptionRef.make(Universe(games))
    } yield SimpleStateManager(uref)
  }
}
