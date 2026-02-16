package gbge.backend.services.state_manager

import gbge.shared.tm.ActionStackInTransit
import zio.IO

trait TimeMachine extends StateManager {
  def reset(number: Int): IO[Unit, Unit]
  val actionsAndInvokers: IO[Nothing, ActionStackInTransit]
}
