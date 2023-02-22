package zeromc.next.plugins.zeroafk.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import zeromc.next.plugins.zeroafk.Zeroafk
import zeromc.next.plugins.zeroafk.Zeroafk.Companion.PLUGIN
import zeromc.next.plugins.zeroafk.utils.tac

class AFKCommand(
    override val name: String,
    override val description: String,
    override val usage: String,
    override val extendedHelp: String,
    override val hidden: Boolean,
    override val requiredOP: Boolean
) : ICommand{
    override fun execute(sender: CommandSender, args: Array<String>) {
        if(hidden){
            sender.msg("&cQuesto comando non esiste")
            return
        }
        val msg = Zeroafk.messages
        val commands = msg["commands"] as Map<String, String>

        if(sender !is Player) {
            val notPlayerMessage:String = Zeroafk.placeholder(commands["not player"] ?: "null")
            sender.msg(notPlayerMessage)
            return
        }
        val player = sender as Player
        if(!player.isOp && requiredOP) {
            val notAuthMessage:String = Zeroafk.placeholder(commands["not allowed"] ?: "null")
            player.msg(notAuthMessage)
            return
        }
        player.msg("%PREFIX% Stai andando afk...")
        PLUGIN.connectPlayerToHub(player)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>? {
        TODO("Not yet implemented")
    }
}

private fun CommandSender.msg(tac: String) {
    sendMessage(Zeroafk.placeholder(tac.tac()))
}
