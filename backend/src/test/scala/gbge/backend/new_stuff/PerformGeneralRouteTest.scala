package gbge.backend.new_stuff

import gbge.backend.endpoints_and_aspects.Aspects
import gbge.backend.gameroutes.GameRoutes
import gbge.backend.models.{Player, Universe}
import gbge.backend.new_stuff.mocks.MockMainService
import gbge.backend.services.MainService
import gbge.shared.actions.{GeneralAction, SelectGame}
import zio.http.*
import zio.json.*
import zio.mock.Expectation
import zio.test.{Assertion, Spec, ZIOSpecDefault, assertTrue}
import zio.{Ref, Scope, ZEnvironment, ZIO}

object PerformGeneralRouteTest extends ZIOSpecDefault {

  private val routeUnderTest: Routes[MainService & Ref[Universe], Nothing] =
    GameRoutes.performGeneralActionRoute.toRoutes @@ Aspects.tokenExtractorAspect

  private def createRequestWithTokenAndAction(token: String, action: GeneralAction) =
    Request.post(URL(Path.root./("performAction")), Body.fromString(action.toJson)).setHeaders(Headers(Header.Authorization.Bearer(token)))

  def spec: Spec[Scope, Any] = suite("successes")(
    test("when provided token is resolved")({
      val mockedMainService = MockMainService.HandleAction(Assertion.anything, Expectation.unit)
      for {
        uref <- Ref.make(Universe(Seq.empty, players = Map("101" -> Player(1, "Steve", true))))
        mms <- mockedMainService.build
        action: GeneralAction = SelectGame(12)
        app = routeUnderTest.provideEnvironment(mms.add(uref))
        response <- app.runZIO(createRequestWithTokenAndAction("101", action))
        a = response.status.isSuccess
        _ <- zio.Console.printLine("was it ok? " + a)
      } yield assertTrue(a)
    })
  )
}
