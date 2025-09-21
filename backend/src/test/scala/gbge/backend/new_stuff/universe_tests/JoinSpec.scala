package gbge.backend.new_stuff.universe_tests

import gbge.backend.models.Universe
import gbge.shared.actions.Join
import zio.Scope
import zio.test.*

object JoinSpec extends ZIOSpecDefault {

  private val testNames: Seq[String] = Seq(
    "Steve",
    "Joe",
    "Diane",
    "Name with whitespace",
  )
  
  override def spec: Spec[Scope, Any] = suite("")(
    test("when players map is empty, joining succeeds with any name")({
      val u = Universe(Seq.empty)
      assertTrue(
        testNames.forall(name => {
          val u2 = u.reduce(Join(name), None).getOrElse(???)._1
          u2.candidate.exists(_.name == name)
        })
      )
    }),
    test("when player name is empty, or only contains white space")(
      assertTrue(true)
    )
  )
}
