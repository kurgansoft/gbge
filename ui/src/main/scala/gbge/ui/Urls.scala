package gbge.ui

object Urls {
  val urlPrefix: String = "../"

  val statePostFix: String = "/api/state/"

  val tmActionsPostFix = "/api/tm/clientTM"

  val tmStatePostFix = "/api/tm/state/"

  val savePostFix = "/api/save/"

  val resetTMPostFix = "/api/resetTM/"

  val stateURL: String = urlPrefix + statePostFix

  lazy val hostname: String = org.scalajs.dom.window.location.host

  lazy val webSocketPrefix: String = if (org.scalajs.dom.window.location.protocol == "https:"){
    "wss://" + hostname
  } else {
    "ws://" + hostname
  }
  lazy val stateSocketURL: String = webSocketPrefix + "/api/stateSocket"
  lazy val portalSocketURL: String = webSocketPrefix + "/api/portalSocket"
  lazy val portalSocketURLForClients: String = webSocketPrefix + "/api/portalSocketForClients/"

  val joinPostFix: String = "/join/"
  val publicEvents: String = "/publicEvents/"
  val performActionPostFix: String = "performAction/"
  val setPortalCoordinatesPostFix: String = "/api/portalCoordinates/"

  val getPlayerPostFix: String = "player/"

  val universeUrl: String = urlPrefix + "api/universe"
}

