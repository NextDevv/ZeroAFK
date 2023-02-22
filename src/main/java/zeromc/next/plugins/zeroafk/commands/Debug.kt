package zeromc.next.plugins.zeroafk.commands

import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import zeromc.next.plugins.zeroafk.Zeroafk
import zeromc.next.plugins.zeroafk.Zeroafk.Companion.PLUGIN
import zeromc.next.plugins.zeroafk.utils.tac

class Debug : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) return true
        val player = sender as Player

        PLUGIN.api.server.whenComplete { name, error ->
            Zeroafk.serverName = name
            PLUGIN.info("Got server name: $name")
        }
        PLUGIN.api.forwardToPlayer("NextDev", "Server", ("d+"+Zeroafk.serverName).toByteArray())
        PLUGIN.api.sendMessage("NextDev", "§cHello world")
        PLUGIN.debug(Zeroafk.playersAfk.toString())
        PLUGIN.api.forward(PLUGIN.config.getString("afk-options.hub-server-name"), "Server", Zeroafk.serverName.toByteArray())
        return true
    }
}

private fun Player.msg(s: String) {
    sendMessage(s.tac())
}
