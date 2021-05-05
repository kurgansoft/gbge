package gbge.ui

object TokenRecoverFactory {
  private val tryToRecoverTokenFromLocalStorage: Boolean = false

  def saveToken(token: String): Unit = {
    if (tryToRecoverTokenFromLocalStorage) {
      org.scalajs.dom.window.localStorage.setItem("token", token)
    } else {
      org.scalajs.dom.window.location.hash = token
    }
  }

  def getToken(): Option[String] = {
    val tokenFromURL = getTokenFromURL()
    if (tokenFromURL.isDefined)
      tokenFromURL
    else {
      if (tryToRecoverTokenFromLocalStorage) {
        getTokenFromLocalStorage()
      } else
        None
    }
  }

  private def getTokenFromURL(): Option[String] = {
    val tokenFormURL: String = org.scalajs.dom.window.location.hash
    if (tokenFormURL != null && tokenFormURL.size > 1 && tokenFormURL.startsWith("#"))
      Option(tokenFormURL.substring(1))
    else
      None
  }

  private def getTokenFromLocalStorage(): Option[String] = {
    val tokenFromLocalStorage: String = org.scalajs.dom.window.localStorage.getItem("token")
    Option(tokenFromLocalStorage)
  }
}
