# Bk Meteor Addon

This meteor addon adds a few useful commands and modules, with some features for the mineplay server (mc.mineplay.nl).

## Modules

 - `PlayerEsp`: Esp for specific players.
 - `PlayerTracers`: Tracers for specific players.
 - `PlayerLoginLogoutNotifier`: Notifies you when a player logs in or out.
 - `MineplayRemoveOfflineRobloxPlayers`: Removes offline roblox players (for mineplay).
 - `MineplayBetterBreak`: Improves block breaking by making it creative-like (for mineplay).
 - `NetworkLoginLogoutNotifier`: Notifies you when a player logs in or out of the network (for mineplay, also may work on other server networks).
 - `BadWordFinder`: Finds bad words in chat messages and nearby signs.
 - `VivecraftVanishDetect`: Detects if a player is in vanish mode using /vr list (the server and the player vanishing must have vivecraft).
 - `MineplayBetterBorder`: Makes the world border have smaller collisions to stop spawn teleporting (for mineplay).

## Commands

 - `.locate-player`: Will temporarily show a tracer to the player for 5 seconds.
 - `.network-online`: Shows online players on the network, gets settings from and requires NetworkLoginLogoutNotifier (for mineplay, also may work on other server networks).
 - `.vivecraft-vanished`: Shows vanished players on the server that have vivecraft, gets settings from and requires VivecraftVanishDetect (the server and the player vanishing must have vivecraft).
 - `.mp-ban`: Will ban a player using mineplay admin ban presets (requires /ban).
 - `.mp-blocks`: Will tell players how to get blocks.
 - `.mp-ip`: Will tell players the mineplay IPs.
 - `.mp-kick`: Will kick a player using mineplay admin kick presets (requires /kick).
 - `.mp-mute`: Will mute a player using mineplay admin mute presets (requires /mute).
 - `.mp-rban`: Will rban a roblox player using mineplay admin rban presets (requires /rban).
 - `.mp-rwarn`: Will warn a roblox player using mineplay admin warn presets.
 - `.mp-warn`: Will warn a player using mineplay admin warn presets (requires /warn).
 - `.bk-update-resources`: Updates the updatable resources of Bk Meteor Addon.

## Other Features
 - Improves meteor-rejects ChatBot to allow you to get the sender with `<sender>`
 - Improves meteor-rejects ChatBot to allow you to get arguments with `<args>` (warning changes the check from the end of the message to contained in the message)
 - Improves meteor-rejects ChatBot to add a message delay option
