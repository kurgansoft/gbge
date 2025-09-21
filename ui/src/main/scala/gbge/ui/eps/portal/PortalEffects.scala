package gbge.ui.eps.portal

import gbge.shared.FrontendUniverse
import gbge.shared.tm._
import gbge.ui.Urls
import org.scalajs.dom.WebSocket
import uiglue.EventLoop.EventHandler
import zio.{UIO, ZIO}

import scala.util.Try

object PortalEffects {
  def setUpPortalWSConnection(id: Int): EventHandler[PortalClientEvent] => UIO[List[PortalClientEvent]] = eventHandler => {
    val portalSocket = new WebSocket(Urls.portalSocketURLForClients + id.toString)
    portalSocket.onmessage = message => {
//      val payload: PortalMessage = upickle.default.read[PortalMessage](message.data.toString)
      val payload: PortalMessage = ???
      payload match {
        case PortalMessageWithPayload(rawFU, perspective) =>
          val fu: FrontendUniverse = ???
          eventHandler(UniversePerspectivePairReceived(perspective, fu))
        case ActionNeedsToBeSelected =>
          eventHandler(ActionNeedsToBeSelectedEvent)
        case PerspectiveNeedsToBeSelected(selectedAction) =>
          eventHandler(PerspectiveNeedsToBeSelectedEvent(selectedAction))
      }
    }
    ZIO.succeed(List.empty)
  }

  val retrievePortalIdFromHash: UIO[List[PortalClientEvent]] = {
    val hash: String = org.scalajs.dom.window.location.hash
    val portalId: Option[Int] = if (hash.length > 1) {
      val t: String = hash.substring(1)
      val t2 = Try(Integer.parseInt(t))
      t2.map(Some(_)).getOrElse(None)
    } else {
      None
    }
    if (portalId.isDefined) {
      ZIO.succeed(List(gbge.ui.eps.portal.PortalId(portalId.get)))
    } else
      ZIO.succeed(List.empty)
  }
}
