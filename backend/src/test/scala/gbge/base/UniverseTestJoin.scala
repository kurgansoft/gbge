package gbge.base

import gbge.backend.{ExecuteEffect, Failure, Player, Universe}
import org.scalatest.funsuite.AnyFunSuite
import gbge.shared.actions.Join

class UniverseTestJoin extends AnyFunSuite {

  def invariant(universe: Universe): Unit = {
    if (universe.game.isDefined) {
      assert(universe.selectedGame.isDefined)
    }
    if (universe.players.nonEmpty) {
      universe.players.exists(_.isAdmin)
    }
  }

  val player1: Player = Player(1, "Steve", "token1")
  val player2: Player = Player(2, "Joseph", "token2", isAdmin = true)
  val player3: Player = Player(3, "A", "token3")
  val player4: Player = Player(4, "B", "token4")
  val player5: Player = Player(5, "C", "token5")
  val player6: Player = Player(6, "D", "token6")
  val player7: Player = Player(7, "E", "token7")
  val player8: Player = Player(8, "F", "token8")
  val player9: Player = Player(9, "G", "token9")
  val player10: Player = Player(10, "H", "token10")
  val player11: Player = Player(11, "X", "token11")
  val player12: Player = Player(12, "Y", "token12")

  test("JOIN; universe is full") {
    val universe = Universe(players = List(player1, player2, player3, player4, player5, player6, player7, player8, player9, player10, player11, player12))
    val result = universe.reduce(Join("William"))
    assert(!result._1.joinInProgress)
    assert(result._2.isInstanceOf[Failure])
  }

  test("JOIN; name is taken already - ignorecase") {
    val universe = Universe(players = List(player2))
    val result = universe.reduce(Join("JOSeph"))
    assert(!result._1.joinInProgress)
    assert(result._2.isInstanceOf[Failure])
  }

  test("JOIN; cool") {
    val universe = Universe(players = List(player2))
    val result = universe.reduce(Join("Steve"))
    assert(result._1.joinInProgress)
    assert(result._2.isInstanceOf[ExecuteEffect])
  }
}
