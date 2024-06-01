package gbge.ui.state.screenstates

import org.scalatest.funsuite.AnyFunSuite

class JoinScreenStateTest extends AnyFunSuite {
  test("one") {
    val joinScreenState = JoinScreenState()
    val joinScreenState2 = joinScreenState.handleScreenEvent(NameInput("Person"))
    assert(joinScreenState2._1.asInstanceOf[JoinScreenState].nameInput == "Person" && joinScreenState2._1.asInstanceOf[JoinScreenState].submitEnabled)
  }
}
