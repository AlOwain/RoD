# Reset on Death (ROD)
> This plugin attempts to improve the multiplayer hardcore minecraft experience by resetting the world automatically when a player dies, and keeping count of all player deaths.

This plugin works by creating two worlds, a main world named "world" which stores a scoreboard of all player deaths and (possibly) gamerules, and a world "hardcore" that players are moved from and into when a death occurs.

## Bugs

- The user who has died, drops all items at spawn.
- It currently moves all users to the limbo world, not resetting the world.

## Missing Features

- Players earn an extra shared-life when killing the Ender Dragon.
- Temporary blindness and a sound effect with text when a player dies.
- Creation of archives instead of world deletion.
- Players spawning into the limbo world are not automatically teleported into "hardcore", as clearing their inventory and experience could be necessary, and sometimes not. Further experimentation of the behavior is required.
