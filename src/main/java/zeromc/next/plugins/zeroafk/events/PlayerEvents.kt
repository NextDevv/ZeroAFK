package zeromc.next.plugins.zeroafk.events

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.metadata.MetadataValue
import org.bukkit.scoreboard.DisplaySlot
import sun.audio.AudioPlayer.player
import zeromc.next.plugins.zeroafk.Zeroafk
import zeromc.next.plugins.zeroafk.Zeroafk.Companion.PLUGIN


class PlayerEvents:Listener {
    @EventHandler
    fun onPlayerMovement(e: PlayerMoveEvent) {
        if(!Zeroafk.IS_HUB_PLUGIN)
            Zeroafk.playerMovements[e.player.uniqueId] = hashMapOf(
                "movement" to System.currentTimeMillis()
            )
        else {
            PLUGIN.debug("Contains: ${Zeroafk.playersAfk.contains(e.player.uniqueId)}")
            if(Zeroafk.playersAfk.contains(e.player.uniqueId)) {
                PLUGIN.api.connect(
                    e.player,
                    Zeroafk.playersAfk[e.player.uniqueId]
                )
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        e.player.resetTitle()
        PLUGIN.api.server.whenComplete { name, error ->
            Zeroafk.serverName = name
            PLUGIN.info("Got server name: $name")
        }


        // Create a scoreboard
        val scoreboard = Bukkit.getScoreboardManager()?.newScoreboard
        val sidebar = scoreboard?.registerNewObjective("sidebar", "dummy")
        sidebar?.displaySlot = DisplaySlot.SIDEBAR
        val score = sidebar?.getScore(PlaceholderAPI.setPlaceholders(e.player,"afk1 %afk%"))
        score?.score = 0
        val score2 = sidebar?.getScore(PlaceholderAPI.setPlaceholders(e.player,"afk2 %afk_afk%"))
        score2?.score = 0
        val score3 = sidebar?.getScore(PlaceholderAPI.setPlaceholders(e.player,"afk3 %afk-afk%"))
        score3?.score = 0

        if (scoreboard != null) {
            e.player.scoreboard = scoreboard
        }
    }

    @EventHandler
    fun onPlayerChat(e: PlayerChatEvent) {
        if(!Zeroafk.IS_HUB_PLUGIN)
            Zeroafk.playerMovements[e.player.uniqueId] = hashMapOf(
                "chat" to System.currentTimeMillis()
            )
        else {
            if(Zeroafk.playersAfk.contains(e.player.uniqueId)) {
                PLUGIN.api.connect(
                    e.player,
                    Zeroafk.playersAfk[e.player.uniqueId]
                )
            }
        }
    }
}