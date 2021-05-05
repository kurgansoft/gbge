package gbge.backend

import gbge.shared.{ClientTimeMachine, FrontendUniverse}
import gbge.shared.actions.{Action, GeneralAction}
import gbge.shared.tm._

import scala.collection.mutable.ListBuffer
import scala.util.Try

case class TimeMachine(
                        changePoints: List[Int] = List.empty,
                        actions: List[(Action, Option[String])] = List.empty, // action & playerToken
                        universes: List[(Universe, Boolean)] = List((Universe(), true)), // universe & validity
                        frontendUniverses: List[Map[Perspective, FrontendUniverse]] = List(TimeMachine.getFUsForGivenUniverse(Universe())),
                      ) {
  assert(universes.size == frontendUniverses.size)
  assert(actions.size == universes.size - 1)

  def take(number: Int): TimeMachine = {
    assert(number >= 0)
    assert(number <= actions.size)
    this.copy(
      changePoints.filterNot(_ > number),
      actions = this.actions.take(number),
      universes = this.universes.take(number + 1),
      frontendUniverses = this.frontendUniverses.take(number + 1)
    )
  }

  def convertToClientTimeMachine(): ClientTimeMachine = {
    val actionsWithResults = actions.map(_._1).zip(universes.slice(1, universes.size).map(_._2))
    val preRenderedFUs: List[FrontendUniverse] = changePoints.map(number => frontendUniverses(number)(SpectatorPerspective))
    val result = ClientTimeMachine(changePoints, actionsWithResults,
      stateCache = changePoints.map(number => (number, SpectatorPerspective)).zip(preRenderedFUs).toMap
    )
    result
  }

  def addAction(action: Action, playerToken: Option[String] = None): TimeMachine = {
    val newEntry = (action, playerToken)
    val newUniverse = universes.last._1.reduce(action, playerToken)
    val success = newUniverse._2.isInstanceOf[Success]
    val newFUs = TimeMachine.getFUsForGivenUniverse(newUniverse._1)

    val newChangePoints = {
      if (universes.size <= 1 || TimeMachine.shouldNewChangePointToBeAdded(universes.last._1.game, newUniverse._1.game)) {
        changePoints.appended(universes.size + 1)
      } else {
        changePoints
      }
    }

    TimeMachine(
      newChangePoints,
      actions ::: List(newEntry),
      universes ::: List((newUniverse._1, success)),
      frontendUniverses ::: List(newFUs)
    )
  }

  lazy val latestUniverse: (Universe, Boolean) = universes.last

  def getFrontendUniverseForPerspective(number: Int, perspective: Perspective) : Either[String, (FrontendUniverse, Boolean)] = {
    if (number < 0 || number > actions.size) {
      Left("Invalid number.")
    } else {
      val fu = frontendUniverses(number).get(perspective)
      if (fu.isEmpty)
        Left("Non-existent player.")
      else
        Right((fu.get, universes(number)._2))
    }
  }

  def serialize(): String = {
    val serializedActions: List[(String, Option[String])] = actions.map(t => (t._1.serialize(), t._2))
    upickle.default.write(serializedActions)
  }
}

object TimeMachine {

  def shouldNewChangePointToBeAdded(previousGame: Option[BackendGame[_]] , newGame: Option[BackendGame[_]]): Boolean = {
    if (newGame.isEmpty) {
      false
    } else if (previousGame.isEmpty) {
      true
    } else {
      previousGame.get.getClass != newGame.get.getClass
    }
  }

  def getFUsForGivenUniverse(universe: Universe): Map[Perspective, FrontendUniverse] = {
    val perspectives: List[Perspective] = SpectatorPerspective :: universe.players.map(x => PlayerPerspective(x.id))
    val list: List[(Perspective, FrontendUniverse)] = perspectives.map(perspective => {
      val o: Option[Int] = perspective match {
        case SpectatorPerspective => None
        case PlayerPerspective(playerId) => Some(playerId)
      }
      (perspective, universe.getFrontendUniverseForPlayer(o))
    })
    list.toMap
  }

  def decode(raw: String): TimeMachine = {
    val serializedActionsAndTokens: List[(String, Option[String])] = upickle.default.read[List[(String, Option[String])]](raw)
    val universes: ListBuffer[(Universe, Boolean)] = new ListBuffer[(Universe, Boolean)]()
    val actionsAndTokens: ListBuffer[(Action, Option[String])] = new ListBuffer[(Action, Option[String])]()
    universes.addOne(Universe(), true)
    var changePoints: List[Int] = List.empty

    for (t <- serializedActionsAndTokens.zipWithIndex) {
      val index = t._2
      val serializedAction: String = t._1._1
      val token: Option[String] = t._1._2

      val action = {
        val generalAction = Try(upickle.default.read[GeneralAction](serializedAction))
        if (generalAction.isSuccess) {
          generalAction.get
        } else {
          val currentGame = universes(index)._1.game.get
          val action = currentGame.decodeAction(serializedAction)
          action
        }
      }

      val universeAndResultPair = universes(index)._1.reduce(action, token)
      val booleanResult = universeAndResultPair._2.isInstanceOf[Success]
      universes.addOne((universeAndResultPair._1, booleanResult))
      actionsAndTokens.addOne((action, token))

      if (shouldNewChangePointToBeAdded(universes(index)._1.game, universeAndResultPair._1.game)) {
        changePoints = changePoints.appended(index+1)
      }
    }
    TimeMachine(changePoints, actionsAndTokens.toList, universes.toList, universes.map(uPair => TimeMachine.getFUsForGivenUniverse(uPair._1)).toList)
  }
}
