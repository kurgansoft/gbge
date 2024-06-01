package gbge.backend

import org.scalatest.funsuite.AnyFunSuite

class MainControllerTest extends AnyFunSuite {
  test("loadPath property not set") {
    val mc = new MainController
    assert(mc.timeMachine.actions.isEmpty)
  }

  test("loadPath property is invalid ") {
    System.setProperty("loadPath", "non_existent.dat")
    val mc = new MainController
    assert(mc.timeMachine.actions.isEmpty)
  }

  test("loadPath property is valid, corrupt data ") {
    System.setProperty("loadPath", "corrupt.dat")
    val mc = new MainController
    assert(mc.timeMachine.actions.isEmpty)
  }

  //TODO Should save_game.dat be bundled with the test?
  //  test("loadPath property is valid") {
  //    System.setProperty("loadPath", "save_game.dat")
  //    val mc = new MainController
  //    assert(mc.timeMachine.actions.nonEmpty)
  //  }
}