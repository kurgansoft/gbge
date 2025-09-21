package gbge.backend

import gbge.shared.tm.PortalCoordinates

class Portal(
              val id: Int,
//              var controllers: List[WebSocketChannel],
//              var clients: List[WebSocketChannel] = List.empty,
              var coordinates: Option[PortalCoordinates] = None
          )
