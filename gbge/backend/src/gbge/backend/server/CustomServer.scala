package gbge.backend.server

import cask.main.Routes
import gbge.backend.MainController
import gbge.backend.gameroutes.{CustomJSFileRoute, GameRoutes, StandardJSFileRoute, TMRoutes}

import scala.util.Try

abstract class CustomServer() extends cask.Main {

  protected lazy val mainController: MainController = new MainController()

  val jsLocation: Option[String] = None

  override def port: Int = {
    val portAsString = System.getProperty("port", "8080")
    Try(Integer.parseInt(portAsString)).getOrElse(8080)
  }

  override def host: String = System.getProperty("hostAddress", "localhost")

  private lazy val theJSRoute: cask.main.Routes = jsLocation match {
    case None => new StandardJSFileRoute()
    case Some(jsFileRoute) => new CustomJSFileRoute(jsFileRoute)
  }

  override def allRoutes: Seq[Routes] = Seq(
    GameRoutes(mainController),
    TMRoutes(mainController),
    theJSRoute
  )
}