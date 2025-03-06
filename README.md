# Reset on Death (ROD)
> This plugin attempts to improve the multiplayer hardcore minecraft experience by resetting the world automatically when a player dies, and keeping count of all player deaths.

This plugin works by creating two worlds, a main world named "world" which stores a scoreboard of all player deaths and (possibly) gamerules, and a world "hardcore" that players are moved from and into when a death occurs.

## Bugs

- If a user dies, they drop all their items at the new spawn.

## Missing Features

- Players earn an extra shared-life when killing the Ender Dragon.
- Temporary blindness and a sound effect with text when a player dies.
- Creation of archives instead of world deletion.
- Players spawning into the limbo world are automatically teleported into "hardcore", however, clearing their inventory and experience is necessary. Whether this can clear players' inventories without any instigation from them is yet to be seen. Further experimentation of the behavior is required.
