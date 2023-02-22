package zeromc.next.plugins.zeroafk.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import zeromc.next.plugins.zeroafk.Zeroafk

sealed interface ICommand : CommandExecutor, TabExecutor{
    val name: String
    val description: String
    val usage: String
    val extendedHelp: String
    val hidden: Boolean
    val requiredOP: Boolean

    fun execute(sender: CommandSender, args: Array<String>)
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>?

    fun register(): Boolean {
        val command = Zeroafk.PLUGIN.getCommand(name)
        command?.setExecutor(this)
        command?.tabCompleter = this
        return command != null
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        execute(sender, (args ?: arrayOf()) as Array<String>)
        return true
    }
}