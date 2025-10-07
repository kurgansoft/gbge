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

  private val resourceRoutes = Routes.empty @@ Middleware.serveResources(Path.empty, "gbge/ui")

  private val staticAssetsRoute: Routes[Any, Nothing] = {
    if (devStaticRouteOptions.isEmpty)
      resourceRoutes
    else {
      val middlewares = devStaticRouteOptions.get.staticFileFolders.map({ case (pathPrefix, filePath) =>
        Middleware.serveDirectory(Path(pathPrefix), new File(filePath))
      })
      middlewares.foldLeft(resourceRoutes)((routesAcc, middleware) => routesAcc @@ middleware)
    }
  }
  
  val allStaticRoutes = Routes(redirect1, redirect2, mainJSRoute) ++ staticAssetsRoute

}

object StaticRoutes {
  
  private type PathPrefix = String
  private type FilePath = String
  
  case class DevStaticRouteOptions(
    mainFilePath: String,
    staticFileFolders: Map[PathPrefix, FilePath] = Map.empty
  )
}
