package gbge.backend

import gbge.shared.actions.{DelegateAdminRole, Join, ProvideToken}
import org.scalatest.funsuite.AnyFunSuite

class DelegateAdminRoleTest extends AnyFunSuite {

  val universe: Universe = Universe()
    .reduce(Join("player1"))._1
    .reduce(ProvideToken("t1"))._1
    .reduce(Join("player2"))._1
    .reduce(ProvideToken("t2"))._1
    .reduce(Join("player3"))._1
    .reduce(ProvideToken("t3"))._1


  test("caller is admin, target id exists") {
    val (newUniverse, result) = universe.reduce(DelegateAdminRole(2), Some("t1"))
    assert(result.isInstanceOf[Success])
    assert(newUniverse.players.find(_.isAdmin).exists(_.id == 2))
  }

  test("caller is non-admin") {
    val result1 = universe.reduce(DelegateAdminRole(1), Some("t2"))._2
    val result2 = universe.reduce(DelegateAdminRole(11), Some("t2"))._2
    assert(result1.isInstanceOf[UnauthorizedFailure])
    assert(result2.isInstanceOf[UnauthorizedFailure])
  }

  test("caller is admin, target id is self") {
    val (newUniverse, result) = universe.reduce(DelegateAdminRole(1), Some("t1"))
    assert(newUniverse == universe)
    assert(result.isInstanceOf[Failure])
  }

  test("caller is admin, target id does not exist") {
    val (newUniverse, result) = universe.reduce(DelegateAdminRole(11), Some("t1"))
    assert(newUniverse == universe)
    assert(result.isInstanceOf[Failure])
  }
}