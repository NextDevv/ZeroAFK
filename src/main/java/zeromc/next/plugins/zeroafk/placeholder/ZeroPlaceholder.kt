package zeromc.next.plugins.zeroafk.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import zeromc.next.plugins.zeroafk.Zeroafk

class ZeroPlaceholder : PlaceholderExpansion() {

    override fun canRegister(): Boolean {
        return true
    }

    override fun getIdentifier(): String {
        return "afk"
    }

    override fun persist(): Boolean {
        return true // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    override fun getAuthor(): String {
        return "NextDev"
    }

    override fun getVersion(): String {
        return "1.0-SNAPSHOT"
    }

    override fun onPlaceholderRequest(player: Player?, params: String): String {
        if(params == "afk") {
            val time = Zeroafk.database[player!!.uniqueId.toString()] ?: 0L
            // Format the long to a formatted time string
            val seconds = time / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return String.format("%02d:%02d:%02d", days, hours % 24, minutes % 60)
        }
        return "null"
    }
}