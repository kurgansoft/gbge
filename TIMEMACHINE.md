# Time Machine

The Time Machine is a developer tool, providing a visualization to see 
how the state of the application has changed over time.
By default, it is available at the below URL:

http://localhost:8080/tm

Clicking on the SAVE button in the lower right corner saves the state of the game 
into the file '<<time_stamp>>.dat'. The location of the save file is the working directory.

When you launch the application again you can restore a previous state. 
In order to do so provide the below VM option:

```
-DloadPath=save_game.dat
``` 


