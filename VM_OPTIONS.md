Name        |Default value|Description
------------|-------------|-----------
hostAddress |localhost    |The IP address which to the server is bound to. 
port        |8080         |The port which to the server is bound to.
loadPath    | -           |A location to a file from which a game state can be recovered.
randomToken |true         |A boolean value which determines if tokens should be assigned randomly or sequentially starting from 101.(Value false is useful during development.)
tmEnabled   |false        |Whether or not the time machine is enabled. During an actual game it must be false, otherwise cheating may occur.
