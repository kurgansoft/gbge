package gbge.backend.services.state_manager

import gbge.backend.Failure
import gbge.backend.models.Universe
import gbge.backend.services.TokenGenerator
import gbge.shared.actions.Action
import zio.ZIO

case class TimeMachineContent(
  original: Universe,
  actionStack: Seq[(Action, Option[Int])] = Seq.empty,
  universes: Seq[Universe] = Seq.empty
) {
  def reduce(action: Action, playerId: Option[Int]): Either[Failure, (TimeMachineContent, ZIO[TokenGenerator, Failure, Seq[Action]])] =
    latestUniverse.reduce(action, playerId) match
      case Left(failure) => Left(failure)
      case Right(newUniverse, effect) => Right(this.copy(
        actionStack = actionStack.appended((action, playerId)),
        universes.appended(newUniverse)
      ), effect)

  lazy val latestUniverse: Universe = universes.lastOption.getOrElse(original)
}
