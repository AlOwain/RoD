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

fun tp(to: World) {
    val spawn = to.getSpawnLocation()
    check(spawn.isWorldLoaded()) {
        "'${to.name}' is not loaded."
    }

    for (player in Bukkit.getOnlinePlayers()) {
        check(player.teleport(spawn)) {
            "Teleporting player '${player.getName()}' to ${spawn.getWorld().name} (${spawn.getX()}, ${spawn.getY()}, ${spawn.getZ()}) failed."
        }
    }
}

fun reset_player(player: Player, gamemode: GameMode, spawn: Location) {
    player.setRespawnLocation(spawn)

    player.getEnderChest().clear();
    player.setHealth(20.0);
    player.setFoodLevel(20);
    player.setSaturation(5.0f);
    player.getInventory().clear()
    player.setTotalExperience(0)
    player.setLevel(0)

    player.setGameMode(gamemode)

    for (advancement in Bukkit.advancementIterator()) {
        val progress = player.getAdvancementProgress(advancement)
        for (criteria in progress.awardedCriteria) {
            progress.revokeCriteria(criteria)
        }
    }
}

fun reset_all(gamemode: GameMode, spawn: Location) {
    for (player in Bukkit.getOnlinePlayers()) {
        reset_player(player, gamemode, spawn)
    }
}

fun regenerate(): World {
    val old_hardcore = Bukkit.getWorld("hardcore")
    if (old_hardcore != null) {
        rodlog.logger.info("Unloading world")
        Bukkit.unloadWorld(old_hardcore, false)

        rodlog.logger.info("Deleting world '" + old_hardcore.getWorldFolder() + "'")
        old_hardcore.getWorldFolder().deleteRecursively()
    }

    val hardcore = WorldCreator("hardcore").seed(Random().nextLong()).hardcore(true).createWorld()!!
    hardcore.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 1)
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item]")
    val items_before = hardcore.getEntitiesByClass(Item::class.java)
    for (item in items_before) {
        item.remove()
    }

    check(Bukkit.getWorld("hardcore") != null) {
        "Hardcore failed to create"
    }

    return hardcore
} 

class DeathListener : Listener {
    private var resetting: Boolean = false;

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        synchronized(this) {
            if (resetting) return
            resetting = true;
        }
        val world: World = Bukkit.getWorld("world")!!
        reset_all(GameMode.SPECTATOR, world.getSpawnLocation())
        tp(world)

        val hardcore: World = regenerate()
        rodlog.logger.info("Creating world with seed (" + hardcore.getSeed() + ")")

        tp(hardcore)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item]")
        val items_after = hardcore.getEntitiesByClass(Item::class.java)
        for (item in items_after) {
            item.remove()
        }

        reset_all(GameMode.SURVIVAL, hardcore.getSpawnLocation())
        resetting = false
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (event.player.getWorld().name == "world") {
            val spawn: Location = Bukkit.getWorld("hardcore")!!.getSpawnLocation()
            reset_player(event.player, GameMode.SURVIVAL, spawn)
            check(event.player.teleport(spawn)) {
                "Teleporting player '${event.player.getName()}' to hardcore world failed."
            }
        }
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

        if (Bukkit.getWorld("hardcore") == null) {
            regenerate()
        }
    }
}
