[![GitHub Release](https://img.shields.io/github/v/release/Bknibb/bk-meteor-addon)](https://github.com/Bknibb/bk-meteor-addon/releases) [![Discord](https://img.shields.io/discord/1388788639516987563?logo=discord&color=7289DA)](https://discord.gg/3ErWyvPF4G) [![GitHub commit activity](https://img.shields.io/github/commit-activity/m/Bknibb/bk-meteor-addon)](https://github.com/Bknibb/bk-meteor-addon/commits/master/) [![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/Bknibb/bk-meteor-addon)](https://github.com/Bknibb/bk-meteor-addon/issues) [![GitHub Issues or Pull Requests](https://img.shields.io/github/issues-pr/Bknibb/bk-meteor-addon)](https://github.com/Bknibb/bk-meteor-addon/pulls)
# Bk Meteor Addon

This meteor addon adds a few useful commands and modules, with some features for the mineplay server (mc.mineplay.nl).

## Modules

 - `PlayerEsp`: Esp for specific players.
 - `PlayerTracers`: Tracers for specific players.
 - `PlayerLoginLogoutNotifier`: Notifies you when a player logs in or out.
 - `MineplayBetterBreak`: Improves block breaking by making it creative-like (for mineplay).
 - `NetworkLoginLogoutNotifier`: Notifies you when a player logs in or out of the network (for mineplay, also may work on other server networks).
 - `BadWordFinder`: Finds bad words in chat messages and nearby signs.
 - `MineplayBetterBorder`: Makes the world border have smaller collisions to stop spawn teleporting (for mineplay).

## Commands

 - `.locate-player`: Will temporarily show a tracer to the player for 5 seconds.
 - `.network-online`: Shows online players on the network, gets settings from and requires NetworkLoginLogoutNotifier (for mineplay, also may work on other server networks).
 - `.mp-ban`: Will ban a player using mineplay admin ban presets (requires /ban).
 - `.mp-blocks`: Will tell players how to get blocks.
 - `.mp-ip`: Will tell players the mineplay IPs.
 - `.mp-kick`: Will kick a player using mineplay admin kick presets (requires /kick).
 - `.mp-mute`: Will mute a player using mineplay admin mute presets (requires /mute).
 - `.mp-rban`: Will rban a roblox player using mineplay admin rban presets (requires /rban).
 - `.mp-rwarn`: Will warn a roblox player using mineplay admin warn presets.
 - `.mp-rkick`: Will kick a roblox player using mineplay admin kick presets.
 - `.mp-warn`: Will warn a player using mineplay admin warn presets (requires /warn).
 - `.bk-update-resources`: Updates the updatable resources of Bk Meteor Addon.
 - `.bk-update-addon`: Updates Bk Meteor Addon.

## Other Features
 - Improves meteor-rejects ChatBot to allow you to get the sender with `<sender>`
 - Improves meteor-rejects ChatBot to allow you to get arguments with `<args>` (warning changes the check from the end of the message to contained in the message)
 - Improves meteor-rejects ChatBot to add a message delay option
 - Improves meteor-rejects ChatBot to allow you to private message the sender with `<pms>` (or `<pmsipm>` which only does it if it was sent in a private message) (needs `<sender>` to be used)
