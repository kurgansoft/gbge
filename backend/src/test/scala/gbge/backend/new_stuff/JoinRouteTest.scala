package gbge.backend.new_stuff

import gbge.backend.models.Universe
import gbge.backend.new_stuff.mocks.MockTokenGenerator
import gbge.backend.services.state_manager.SimpleStateManager
import gbge.backend.services.{MainService, MainServiceLive, SequentialTokenGenerator}
import gbge.backend.BackendGameProps
import gbge.backend.gameroutes.GameRoutes
import zio.http.*
import zio.mock.Expectation
import zio.test.{Spec, ZIOSpecDefault, assertTrue}
import zio.{Ref, Scope, ZEnvironment}

import java.io.IOException

object JoinRouteTest extends ZIOSpecDefault {

  private val routeUnderTest: Routes[MainService, Nothing] = GameRoutes.joinRoute.toRoutes

  private val mockTokenGenerator = MockTokenGenerator.GenerateToken(Expectation.value("mockToken"))

  private def createRequestWithName(name: String) =
    Request.post(URL(Path.root./("join")), Body.fromString(name))

  def spec: Spec[Scope, Any] = suite("successes")(
    test("???")(
      for {
        universeRef <- Ref.make(Universe(Seq.empty))
        counter <- Ref.make(100)

        tokenGenerator = ZEnvironment(SequentialTokenGenerator(counter))
//        tokenGeneratorLayer: ULayer[SequentialTokenGenerator] = ZLayer.succeed(SequentialTokenGenerator(counter))

        stateManager <- SimpleStateManager.layer.build.provideSomeEnvironment[Scope](scope => scope ++ ZEnvironment(Seq.empty))

        ms <- MainServiceLive.layer.build.provideSomeEnvironment[Scope](scope =>
          scope.add(Seq.empty[BackendGameProps[_,_]]) ++ tokenGenerator ++  stateManager)

        _ <- stateManager.get.universeStream.changes.foreach(u => universeRef.set(u)).fork

        app = routeUnderTest.provideEnvironment(ms)

        universe <- universeRef.get
        _ <- zio.Console.printLine(universe)
        _ = assert(universe.players.isEmpty)

        response <- app.runZIO(createRequestWithName("Steve"))
        _ <- zio.Console.printLine("==> " + response.status)
        _ = assert(response.status == Status.Ok)

        universe <- universeRef.get
        _ = assert(universe.players.size == 1)

        response <- app.runZIO(createRequestWithName("sTEVE"))
        _ = assert(response.status == Status.BadRequest)
        universe <- universeRef.get
        _ = assert(universe.players.size == 1)

      } yield assertTrue(true)
    ),
    test("when universe has 0 players, joining with any name")(
      for {
        universeRef <- Ref.make(Universe(Seq.empty))
        counter <- Ref.make(100)
        tokenGenerator = SequentialTokenGenerator(counter)

        stateManager <- SimpleStateManager.layer.build.provideSomeEnvironment[Scope](scope => scope ++ ZEnvironment(Seq.empty))

        mainService <- MainServiceLive.layer.build.provideSomeEnvironment[Scope](scope =>
          scope.add(Seq.empty[BackendGameProps[_, _]]).add(tokenGenerator) ++  stateManager)

        app = routeUnderTest.provideEnvironment(mainService)

        _ <- stateManager.get.universeStream.changes.foreach(u => universeRef.set(u)).fork

        universe <- universeRef.get
        _ = assert(universe.players.isEmpty)

        response <- app.runZIO(createRequestWithName("Steve"))
        _ = assert(response.status == Status.Ok)
        universe <- universeRef.get
        _ = assert(universe.players.size == 1)

        response <- app.runZIO(createRequestWithName("Helen"))
        _ = assert(response.status == Status.Ok)
        universe <- universeRef.get
        _ = assert(universe.players.size == 2)

        response <- app.runZIO(createRequestWithName("Joe"))
        _ = assert(response.status == Status.Ok)
        universe <- universeRef.get
        _ = assert(universe.players.size == 3)

        response <- app.runZIO(createRequestWithName("Tracy"))
        _ = assert(response.status == Status.Ok)
        universe <- universeRef.get
        _ = assert(universe.players.size == 4)

      } yield assertTrue(true)
    )
  )
}
