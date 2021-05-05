package gbge.ui.eps.player

import gbge.client._

case class StandardStateWrapper(
                        clientState: ClientState = ClientState()
                      ) extends UIState[ClientEvent] {

  implicit def implicitConversion(state: StandardStateWrapper): (StandardStateWrapper, ClientResult) = (state, OK)

  override def processClientEvent(event: ClientEvent): (StandardStateWrapper, ClientResult)  = {
    val temp: (StandardStateWrapper, ClientResult) = event match {
      case DispatchActionWithToken(action) => {
        val theToken = clientState.you.flatMap(_.token)
        if (theToken.isDefined) {
          (this, ExecuteEffect(ClientEffects.submitRestActionWithToken(action, theToken)))
        } else {
          this
        }
      }
      case NewFU(fu) => {
        val (newClientState, effect) = clientState.handleNewFU(fu)
        (this.copy(clientState = newClientState), effect)
      }
      case pe: PlayerEvent => {
        val (newClientState, clientResult) = clientState.processClientEvent(pe)
        (this.copy(clientState = newClientState), clientResult)
      }
      case _ => {
        this
      }
    }
    temp._2 match {
      case PrepareRestActionWithToken(action) => (temp._1, ExecuteEffect(ClientEffects.submitRestActionWithToken(action, clientState.you.flatMap(_.token))))
      case _ => temp
    }
  }
}