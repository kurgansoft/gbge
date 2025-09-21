# GBGE - General BoardGame Engine

 Opinionated Scala/ScalaJs library to rapidly adapt traditional board games to digital devices.
 
 ## How to create your own game? 
 
* In the shared module: 
1. Implement an abstract class which extends gbge.shared.actions.GameAction.
The clients will be sending instances of this class to the server indicating what 
actions they would like to perform.

2. Extending from the gbge.shared.GameRole class define all the possible roles in your game.

3. Create a case class which implements the gbge.shared.FrontendGame trait.
In the backend module you'll have to implement a case class which implements the 
BackendGame trait, so it is a good idea to create an abstract class which contains the 
fields/functionalities shared by the frontend and the backend.

* In the backend module:

1. Create a case class which implements the gbge.backend.BackendGame trait.
Create a companion object which extends the gbge.backend.Startable object.

* In the ui module:

1. Extending from gbge.ui.state.ScreenAction define all the possible actions 
that can be taken from the UI. Note that these actions are NOT the same that you 
have defined in the shared module.

2. Extending from gbge.ui.state.OfflineState implement the offline state of your UI.

3. Create an object which extends UIExport ==> ClientGameProps

4. The UI assets (images, sounds, etc) should be put in the backend module under
resources/ui/games/<gamename>

### Creating a launcher

Once the steps in the previous section are all done your game is ready.
However, you need to register it first. Registration is done in your launcher.
You need to have two launchers: backend and UI

#### The backend launcher:

It should look something like this: 

``` scala
import gbge.backend.server.CustomServer

object Launcher extends CustomServer {
  gbge.shared.RG.registeredGames = List(YourClientGame)
  gbge.backend.RG.registeredGames = List(YourBackendGame)

  assert(gbge.backend.RG.registeredGames.size == gbge.shared.RG.registeredGames.size)
  gbge.backend.RG.registeredGames.zip(gbge.shared.RG.registeredGames).foreach(a => {
    assert(a._1.frontendGame == a._2)
  })

  override val jsLocation: Option[String] = Some("<<path_to_the_compiled_js_file>>")
}
```

You can do other customizations here, for example adding https support. Check out the docs of 
[cask](https://com-lihaoyi.github.io/cask/) and [undertow](http://undertow.io/) for details.

#### The UI launcher:

It should look something like this:

``` scala
@JSExportTopLevel("ep")
object UILauncher extends EntryPoint {
  gbge.shared.RG.registeredGames = List(YourClientGame)
  gbge.ui.RG.registeredGames = List(YourUIExportObject)
  assert(gbge.ui.RG.registeredGames.size == gbge.shared.RG.registeredGames.size)
}
```

