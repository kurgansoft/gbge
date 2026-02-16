package gbge.backend.gameroutes

import gbge.backend.endpoints_and_aspects.TimeMachineEndpoints
import gbge.backend.services.state_manager.{TimeMachine, TimeMachineStateManager}
import zio.ZIO
import zio.http.Route
import zio.json.EncoderOps

object TMRoutes {
  val actionHistoryRoute: Route[TimeMachine, Nothing] =
    TimeMachineEndpoints.actionHistory.implement(_ => for {
      _ <- ZIO.log("actionHistoryRoute endpoint invoked...")
      timeMachine <- ZIO.service[TimeMachine]
      actionsAndInvokers <- timeMachine.actionsAndInvokers
    } yield actionsAndInvokers)

  val getTmSpectatorStateAtTimeRoute: Route[TimeMachineStateManager, Nothing] = TimeMachineEndpoints.getTmSpectatorStateAtTime.implement(universeIndex =>
    for {
      tm <- ZIO.service[TimeMachineStateManager]
      currentTimeMachineContent <- tm.subscriptionRef.get
      maxIndex = currentTimeMachineContent.universes.size
      _ <- ZIO.when(universeIndex < 0)(ZIO.logError("negative index") *> ZIO.fail(()))
      _ <- ZIO.when(universeIndex > maxIndex)(ZIO.logError(s"universeIndex [$universeIndex] is too high") *> ZIO.fail(()))
      universeAtThatTime = universeIndex match {
        case 0 => currentTimeMachineContent.original
        case n => currentTimeMachineContent.universes(n - 1)
      }
      _ = println(s"index: [$universeIndex] ==>\n${pprint.apply(universeAtThatTime).plainText}")
      result = universeAtThatTime.getFrontendUniverseForPlayer(None).encode.toJsonPretty
    } yield result
  )

  val getTmStateAtTimeForPlayerRoute: Route[TimeMachineStateManager, Nothing] = {
    TimeMachineEndpoints.getTmStateAtTimeForPlayer.implement(
      { case (universeIndex, playerId) =>
        for {
          tm <- ZIO.service[TimeMachineStateManager]
          currentTimeMachineContent <- tm.subscriptionRef.get
          maxIndex = currentTimeMachineContent.universes.size
          _ <- ZIO.when(universeIndex < 0)(ZIO.logError("negative index") *> ZIO.fail(()))
          _ <- ZIO.when(universeIndex > maxIndex)(ZIO.logError(s"universeIndex [$universeIndex] is too high") *> ZIO.fail(()))
          universeAtThatTime = universeIndex match {
            case 0 => currentTimeMachineContent.original
            case n => currentTimeMachineContent.universes(n - 1)
          }
          _ <- ZIO.log(s"player id is: [$playerId]")
          result = universeAtThatTime.getFrontendUniverseForPlayer(Some(playerId)).encode.toJsonPretty
        } yield result
      }
    )
  }

    val resetRoute: Route[TimeMachine, Nothing] =
      TimeMachineEndpoints.reset.implement(index => for {
        stateManager <- ZIO.service[TimeMachine]
        _ <- ZIO.log(s"Attempting to reset with index [$index]...")
        _ <- stateManager.reset(index)
        _ <- ZIO.log("Reset succeeded!")
      } yield ())

    import java.text.SimpleDateFormat
    import java.util.Calendar

    private val dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")

    val saveRoute: Route[TimeMachineStateManager, Nothing] =
      TimeMachineEndpoints.save.implement(_ => {
        val now = Calendar.getInstance().getTime
        val dateAsString = dateFormat.format(now)
        val fileName = dateAsString + ".dat"
        for {
          stateManager <- ZIO.service[TimeMachine]
          actionStackInTransit <- stateManager.actionsAndInvokers
          _ = os.write.over(os.pwd / fileName, actionStackInTransit.toJsonPretty)
          _ <- ZIO.log(s"saving current stack: [$actionStackInTransit]")
        } yield ()
      })

}
