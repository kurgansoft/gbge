package gbge.client

import scala.concurrent.{ExecutionContext, Future}
import org.scalajs.dom._
import scala.scalajs.js.{Dynamic, Promise}
import scala.scalajs.js.typedarray.Uint8Array

// Not the most beautiful solution, but it works.
// Problem is that it seems that the approach described here:
// https://sttp.softwaremill.com/en/latest/backends/zio.html#server-sent-events
// is not working with the FetchBackend. Maybe it is something that needs to be fixed in sttp itself?
object SseUtils {

  def createFetchPromise(url: String, token: Option[String] = None): Promise[Response] = {
    val headers = new Headers()
    if (token.nonEmpty)
      headers.append("Authorization", s"Bearer ${token.get}")
    headers.append("Accept", "text/event-stream")

    val init = new RequestInit {}
    init.headers = headers
    fetch(url, init)
  }

  def processStream(
                     streamBody: ReadableStream[Uint8Array],
                     onEventCallback: String => Unit,
     )(implicit ec: ExecutionContext): Future[Unit] = {
    val reader: ReadableStreamReader[_] = streamBody.getReader().asInstanceOf[ReadableStreamReader[_]]
    val decoder = Dynamic.newInstance(Dynamic.global.TextDecoder)()

    def readLoop(): Future[Unit] = for {
      result <- reader.read().toFuture
      _ <- if (result.done) {
        Future.failed(new RuntimeException("The SSE stream was closed."))
      } else {
        val decodedValue = decoder.applyDynamic("decode")(result.value.asInstanceOf[Chunk[Byte]], Dynamic.literal(stream = true))
        onEventCallback(decodedValue.toString)
        readLoop()
      }
    } yield ()

    readLoop()
  }
}
