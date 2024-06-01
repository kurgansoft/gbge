package gbge.backend

import gbge.shared.FrontendPlayer
import gbge.shared.actions.Action
import zio.IO

abstract sealed class UniverseResult

abstract sealed class Failure extends UniverseResult
abstract sealed class Success extends UniverseResult

case object OK extends Success
case class OKWithMessage(message: String) extends Success
case class OKWithPlayerPayload(player: FrontendPlayer) extends Success
case class ExecuteEffect(effect: Universe => IO[String, List[Action]]) extends Success
case class ExecuteAsyncEffect(effect: Universe => IO[String, List[Action]]) extends Success

case object FAIL extends Failure
abstract sealed class FailureWithMessage(val message: String = "") extends Failure
case class GeneralFailure(override val message: String = "") extends FailureWithMessage // 406
case class UnauthorizedFailure(override val message: String = "") extends FailureWithMessage // 401