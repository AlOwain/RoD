package dev.alowain.rod

import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Display
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.*
import java.io.File
import java.util.*
import java.util.logging.Logger
import com.sun.tools.attach.AgentLoadException

object rodlog {
    lateinit var logger: Logger
    fun initialize(plugin: JavaPlugin) {
        logger = plugin.logger
    }
}

fun reset_player(player: Player, gamemode: GameMode, spawn: Location) {
    player.getEnderChest().clear();
    player.setHealth(20.0);
    player.setFoodLevel(20);
    player.setSaturation(5.0f);
    player.getInventory().clear();

    player.setRespawnLocation(spawn)

    if (!player.isDead()) {
        player.setGameMode(gamemode);

        check(player.teleport(spawn)) {
            "Teleporting player '${player.getName()}' to ${spawn.getWorld().name} (${spawn.getX()}, ${spawn.getY()}, ${spawn.getZ()}) failed."
        }
    }
}

fun tp(to: World, gamemode: GameMode) {
    val spawn = to.getSpawnLocation()
    check(spawn.isWorldLoaded()) {
        "'${to.name}' is not loaded."
    }

    for (player in Bukkit.getOnlinePlayers()) {
        reset_player(player, gamemode, spawn)
    }

    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke @a everything")
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "experience set @a 0 levels")
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "experience set @a 0 points")
}

class DeathListener : Listener {
    private var resetting: Boolean = false;

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        synchronized(this) {
            if (resetting) return
            resetting = true;
        }
        val player = event.player

        player.spigot().respawn()
        val limbo: World = WorldCreator("limbo").hardcore(true).createWorld()!!
        tp(limbo, GameMode.SPECTATOR)

        val hardcore: World = regenerate()
        rodlog.logger.info("Creating world with seed (" + hardcore.getSeed() + ")")

        tp(hardcore, GameMode.SURVIVAL)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item]")
        val items_after = hardcore.getEntitiesByClass(Item::class.java)
        for (item in items_after) {
            item.remove()
        }

        resetting = false
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.getWorld().name == "limbo") {
            val spawn: Location = WorldCreator("world").hardcore(true).createWorld()!!.getSpawnLocation()
            reset_player(player, GameMode.SURVIVAL, spawn)
            player.setLevel(0);
            player.setExp(0.0f);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke " + player.getName() + " everything")
        }
    }

    private fun regenerate(): World {
        val old_hardcore = Bukkit.getWorld("world")
        if (old_hardcore != null) {
            rodlog.logger.info("Unloading world")
            Bukkit.unloadWorld(old_hardcore, false)

            rodlog.logger.info("Deleting world '" + old_hardcore.getWorldFolder() + "'")
            old_hardcore.getWorldFolder().deleteRecursively()
        }

        val hardcore = WorldCreator("world").seed(Random().nextLong()).hardcore(true).createWorld()!!
        hardcore.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 1)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item]")
        val items_before = hardcore.getEntitiesByClass(Item::class.java)
        for (item in items_before) {
            item.remove()
        }

        return hardcore
    } 
}

class Rod : JavaPlugin() {
    override fun onEnable() {
        rodlog.initialize(this)
        rodlog.logger.info("Initializing..")

        Bukkit.getPluginManager().registerEvents(DeathListener(), this)

        val scoreboardManager: ScoreboardManager = Bukkit.getScoreboardManager()
        val scoreboard: Scoreboard = scoreboardManager.mainScoreboard

        val objective_name = "death_count"
        var objective: Objective? = scoreboard.getObjective(objective_name)
        if (objective == null) {
            objective = scoreboard.registerNewObjective(objective_name, Criteria.DEATH_COUNT, "Deaths", RenderType.INTEGER)
            rodlog.logger.info("Created leaderboard")
        }

        objective.displaySlot = DisplaySlot.PLAYER_LIST
    }
}


// NOTE: You should test the following
// - Player dies in other world
// - Multiple players die at the same time
// - Inventory, XP, achievements, enderchests of players and the dead player
// - Player joins into another world
//
// NOTE: You should build the following
// - Lives that come with killing the Ender Dragon
// - Temporary blindness and a sound effect with text of who died
