package gbge.backend.services.state_manager

import gbge.backend.models.Universe
import gbge.backend.services.TokenGenerator
import gbge.backend.{BackendGameProps, Failure}
import gbge.shared.actions.Action
import gbge.shared.tm.{ActionStackInTransit, EncodedActionInvokerAndPlayers}
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

  lazy val actionStackInTransit: ActionStackInTransit =
    ActionStackInTransit(
      actionStack.zipWithIndex.toList.map({ case ((action, invoker), index) =>
        EncodedActionInvokerAndPlayers(action.convertToJson(), invoker, universes(index).players.values.map(_.toFrontendPlayer()).toList)
      })
    )
}

object TimeMachineContent {
  def rebuildFromActionStack(games: Seq[BackendGameProps[_,_ ]], actionStack: Seq[(Action, Option[Int])]): TimeMachineContent = {
    val universe = Universe(games)

    val universes = actionStack.foldLeft(Seq(universe))({
      case (universes, (action, optionalPlayerId)) =>
        val latestReproducedUniverse = universes.last.reduce(action, optionalPlayerId).getOrElse(???)._1
        universes.appended(latestReproducedUniverse)
    })

    TimeMachineContent(universe, actionStack, universes.tail)
  }
}
