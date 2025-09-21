package gbge.backend.services.state_manager

import gbge.backend.{Failure, BackendGameProps}
import gbge.backend.models.Universe
import gbge.backend.services.TokenGenerator
import gbge.shared.actions.Action
import zio.{IO, Ref, ZIO, ZLayer}
import zio.stream.{SubscriptionRef, ZPipeline, ZStream}

case class TimeMachineStateManager(
                                    subscriptionRef: SubscriptionRef[TimeMachineContent],
                                  ) extends TimeMachine  {
  override def update(action: Action, playerId: Option[Int]): IO[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])] = for {
    effectRef <- Ref.make[ZIO[TokenGenerator, Failure, Seq[Action]]](ZIO.succeed(Seq.empty))
    updatedTimeMachineContent <- subscriptionRef.updateAndGetZIO(u => {
      u.reduce(action, playerId) match
        case Left(failure) => ZIO.fail(failure)
        case Right((u2, effect)) => for {
          _ <- effectRef.set(effect)
        } yield u2
    })
    effect <- effectRef.get
  } yield (updatedTimeMachineContent.latestUniverse, effect)

  override val universeStream: ZStream[Any, Nothing, Universe] = subscriptionRef.changes.map(_.latestUniverse) >>> ZPipeline.changes

  override def reset(number: Int): IO[Unit, Unit] = number match
    case index if index < 0 => ZIO.log("negative number makes no sense") *> ZIO.fail(())
    case index => for {
      _ <- subscriptionRef.updateZIO(tmContent => {
        if (index > tmContent.actionStack.size)
          ZIO.log(s"Provided index [$index] is too large, size of the stack is [${tmContent.actionStack.size}]") *> ZIO.fail(())
        else
          ZIO.succeed(tmContent.copy(
            actionStack = tmContent.actionStack.take(index),
            universes = tmContent.universes.take(index)
          ))
      })
    } yield ()
}

object TimeMachineStateManager {
  val layer: ZLayer[Seq[BackendGameProps[_,_]], Nothing, TimeMachine] = ZLayer {
    for {
      games <- ZIO.service[Seq[BackendGameProps[_,_]]]
      u = Universe(games)
      uref <- SubscriptionRef.make(TimeMachineContent(u))
    } yield TimeMachineStateManager(uref)
  }
}
