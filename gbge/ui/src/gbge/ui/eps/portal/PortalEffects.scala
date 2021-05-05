package gbge.ui.eps.portal

import gbge.client.AbstractCommander
import gbge.shared.FrontendUniverse
import gbge.shared.tm.Perspective
import gbge.ui.Urls
import org.scalajs.dom.raw.WebSocket
import zio.{UIO, ZIO}

import scala.util.Try

object PortalEffects {
  def setUpPortalWSConnection(id: Int): AbstractCommander[PortalClientEvent] => UIO[List[PortalClientEvent]] = commander => {
    ZIO.effectTotal {
      val portalSocket = new WebSocket(Urls.portalSocketURLForClients + id.toString)
      portalSocket.onmessage = message => {
        val (perspective, raw): (Perspective, String) = upickle.default.read[(Perspective, String)](message.data.toString)
        val newUniverse: FrontendUniverse = FrontendUniverse.decode(raw)
        commander.addAnEventToTheEventQueue(UniversePerspectivePairReceived(perspective, newUniverse))
      }
      List.empty
    }
  }

  val retrievePortalIdFromHash: AbstractCommander[PortalClientEvent] => UIO[List[PortalClientEvent]] = _ => {
    ZIO.effectTotal {
      val hash: String = org.scalajs.dom.window.location.hash
      val portalId: Option[Int] = if (hash.length > 1) {
        val t: String = hash.substring(1)
        val t2 = Try(Integer.parseInt(t))
        t2.map(Some(_)).getOrElse(None)
      } else {
        None
      }
      if (portalId.isDefined) {
        List(PortalId(portalId.get))
      } else
        List.empty
    }
  }
}
