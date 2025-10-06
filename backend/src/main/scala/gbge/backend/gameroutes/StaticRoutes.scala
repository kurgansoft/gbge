package gbge.backend.gameroutes

import gbge.backend.gameroutes.StaticRoutes.DevStaticRouteOptions
import zio.http.*
import zio.http.codec.*
import zio.json.JsonEncoder.*

import java.io.File

case class StaticRoutes(devStaticRouteOptions: Option[DevStaticRouteOptions] = None) {

  private val redirect1: Route[Any, Nothing] = Method.GET / "" -> Handler.succeed(Response.seeOther(url"index.html"))
  private val redirect2: Route[Any, Nothing] = Method.GET / "s" -> Handler.succeed(Response.seeOther(url"spectator.html"))

  private val mainJSHandler: Handler[Any, Response, Any, Response] =
    if (devStaticRouteOptions.isEmpty)
      Handler.fromResource("ui-opt.js", Charsets.Utf8).mapError(_ => Response.notFound)
    else {
      val mainFile = new File(devStaticRouteOptions.get.mainFilePath)
      Handler.fromFile(mainFile).mapError(_ => Response.notFound)
    }

  private val mainJSRoute: Route[Any, Nothing] = Method.GET / "main.js" -> mainJSHandler

  private val mw = Middleware.serveResources(Path.empty, "gbge/ui")

  private val staticAssetsRoute: Routes[Any, Nothing] = {
    val resourcesRoute = Routes.empty @@ mw
    if (devStaticRouteOptions.isEmpty)
      resourcesRoute
    else {
      val mw2 = Middleware.serveDirectory(Path.empty,
        new File(devStaticRouteOptions.get.staticFilesFolder)
      )
      resourcesRoute @@ mw2
    }
  }
  
  val allStaticRoutes = Routes(redirect1, redirect2, mainJSRoute) ++ staticAssetsRoute

}

object StaticRoutes {
  case class DevStaticRouteOptions(
    mainFilePath: String,
    staticFilesFolder: String
  )
}
