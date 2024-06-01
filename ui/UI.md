# Steps to create a UI for a game

* Create your offline events extending ScreenEvent
* Create your CustomOfflineState extending OfflineState
* Create you custom UIExport object; make sure you set up your custom offline state
  in the handleNewFU method.
* Create your UILauncher object (@JSExportTopLevel) 