package gbge.backend.services.state_manager

import zio.IO

trait TimeMachine extends StateManager {
  def reset(number: Int): IO[Unit, Unit]
}
