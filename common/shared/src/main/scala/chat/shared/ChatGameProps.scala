package chat.shared

import gbge.shared.GameProps
import zio.json.JsonCodec

trait ChatGameProps extends GameProps[ChatAction, ClientChatGame] {
  override val name: String = "Chat"
  override val urlFragment: String = "chat"

  override val actionCodec: JsonCodec[ChatAction] = ChatAction.codec
  override val gameCodec: JsonCodec[ClientChatGame] = ClientChatGame.codec

}
