package gbge.backend.gameroutes

import cask.util.Logger
import sourcecode.{File, Line, Text}

class CustomJSFileRoute(val jsLocation: String) extends cask.main.Routes {
  implicit def log: Logger = new Logger {
    override def exception(t: Throwable): Unit = println("Exception in CustomJSFileRoute: " + t)

    override def debug(t: Text[Any])(implicit f: File, line: Line): Unit = println("Debug statement in CustomJSFileRoute: " +  t)
  }

  @cask.staticFiles("/js/", headers = Seq("Content-Type" -> "text/javascript"))
  def jsRoute() = jsLocation

  initialize()
}
