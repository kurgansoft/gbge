package gbge.shared

import gbge.shared.actions.{Action, GeneralAction}
import gbge.shared.tm.{Perspective, SpectatorPerspective}

import scala.util.Try

case class ClientTimeMachine(
                              changePoints: List[Int] = List.empty,
                              actions: List[(Action, Boolean)] = List.empty,
                              stateCache: Map[(Int, Perspective), FrontendUniverse] = Map.empty
                            ) {

  def take(number: Int): ClientTimeMachine = {
    assert(number >= 0)
    assert(number <= actions.size)
    this.copy(
      changePoints.filterNot(_ > number),
      actions = this.actions.take(number),
      stateCache = this.stateCache.filter(_._1._1 < number+1)
    )
  }

  def serialize(): String = {
    val actionStrings: List[(String, Boolean)] = actions.map(t => (t._1.serialize(), t._2))
    val serializedStateCache: Map[(Int, Perspective), String] = stateCache.view.mapValues(_.serialize()).toMap
    upickle.default.write((changePoints, actionStrings, serializedStateCache))
  }
}

object ClientTimeMachine {
  def decode(raw: String): ClientTimeMachine = {
    val (changePoints, actionStrings, serializedStateCache) = upickle.default.read[(
      List[Int],
      List[(String, Boolean)],
      Map[(Int, Perspective), String]
    )](raw)

    val stateCache = serializedStateCache.view.mapValues(fu => FrontendUniverse.decode(fu)).toMap
    val actions: List[Action] = for (x <- actionStrings.zipWithIndex) yield {
      val generalAction = Try(upickle.default.read[GeneralAction](x._1._1))
      if (generalAction.isSuccess) {
        generalAction.get
      } else {
        val currentFuNumber: Int = changePoints.filter(_ <= x._2+1).max
        val fu: FrontendUniverse = stateCache((currentFuNumber, SpectatorPerspective))
        fu.game.get.decodeAction(x._1._1)
      }
    }
    ClientTimeMachine(changePoints, actions.zip(actionStrings.map(_._2)), stateCache)
  }
}