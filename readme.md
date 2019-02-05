# Thread Dump Plugin

This plugin is intended to dump threads to a file in the situation where the server has been empty for more than X 
minutes, or the server has failed to report how many players are online inside Y seconds.

This is useful if the server doesn't believe it's been frozen, but is still running yet doesn't respond - Which means
 no Thread Dumps can be triggered by SpigotMC.

This plugin creates a monitoring thread on startup, it starts another thread every interval in which a 
BukkitTask runs and checks on the server.

The reason it doesn't start the BukkitTask inside the first thread is because the BukkitTask itself could potentially
 freeze the thread as it's a part of what we're monitoring.
 
As of the time I write this, this shouldn't be true. But the future is uncertain.