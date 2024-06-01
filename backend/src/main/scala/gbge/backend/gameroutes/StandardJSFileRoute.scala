package gbge.backend.gameroutes

import cask.util.Logger
import sourcecode.{File, Line, Text}

class StandardJSFileRoute() extends cask.main.Routes {
  implicit def log: Logger = new Logger {
    override def exception(t: Throwable): Unit = println("Exception in StandardJSFileRoute: " + t)

    override def debug(t: Text[Any])(implicit f: File, line: Line): Unit = println("Debug statement in StandardJSFileRoute: " +  t)
  }

  @cask.staticResources("/js/")
  def jsRoute() = "gbge/ui/generatedJSFiles"

  initialize()
}
