package zeromc.next.plugins.zeroafk

import JsonFile.JsonFile
import com.google.common.io.ByteStreams
import io.github.leonardosnt.bungeechannelapi.BungeeChannelApi
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.scheduler.BukkitRunnable
import zeromc.next.plugins.zeroafk.commands.AFKCommand
import zeromc.next.plugins.zeroafk.commands.Debug
import zeromc.next.plugins.zeroafk.db.ZeroDatabase
import zeromc.next.plugins.zeroafk.events.PlayerEvents
import zeromc.next.plugins.zeroafk.logger.FileLogger
import zeromc.next.plugins.zeroafk.logger.LogLevel
import zeromc.next.plugins.zeroafk.placeholder.ZeroPlaceholder
import zeromc.next.plugins.zeroafk.utils.tac
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class Zeroafk : JavaPlugin(), PluginMessageListener {
    private val version = Bukkit.getBukkitVersion().split("-")[0];
    var api: BungeeChannelApi by Delegates.notNull()

    companion object {
        var PLUGIN:Zeroafk by Delegates.notNull()
        var IS_HUB_PLUGIN:Boolean = false
        var playerMovements:HashMap<UUID, HashMap<String, Long>> = hashMapOf()
        var playersAfk:HashMap<UUID, String> = hashMapOf()
        var serverName:String = "Unknown"
        var messages:JsonFile by Delegates.notNull()
        var database:ZeroDatabase by Delegates.notNull()

        class Config {
            companion object {
                private val config = PLUGIN.config
                    get() { return field }

                operator fun get(key: String): Any? = config[key]
                operator fun set(key: String, value: Any?) { config[key] = value }
            }
        }

        class Messages {
            companion object {
                operator fun get(key: String):String {
                    val jsf = JsonFile["messages"]
                    return placeholder( jsf?.get(key)?.toString() ?: "null" )
                }

                fun getMap(key: String): Map<String, String> {
                    val jsf = JsonFile["messages"] ?: return emptyMap()
                    return try { ((jsf[key] as Map<String, Any>) as Map<String, String>) }
                    catch (e: Exception) { emptyMap() }
                }
            }
        }

        fun placeholder(string: String):String {
            var s = string
            s = s.replace("%VERSION%", PLUGIN.version)
            s = s.replace("%PREFIX%", Config["prefix"].toString().tac())
            s = s.replace("%COLOR%", Config["color-prefix"].toString().tac())
            return s.tac()
        }
    }

    fun info(message: String) {
        logger.info(message)
        FileLogger.logIntoFile(LogLevel.INFO, message)
    }

    fun warn(message: String) {
        logger.warning(message)
        FileLogger.logIntoFile(LogLevel.WARN, message)
    }

    fun error(message: String) {
        logger.severe(message)
        FileLogger.logIntoFile(LogLevel.ERROR, message)
    }

    fun debug(message: String) {
        logger.fine(message)
        FileLogger.logIntoFile(LogLevel.DEBUG, message)
    }

    fun trace(message: String, throwable: Throwable?) {
        logger.severe(message)
        FileLogger.logIntoFile(LogLevel.TRACE, message, throwable)
    }

    override fun onEnable() {
        // Plugin startup logic
        PLUGIN = this
        FileLogger.setPath(dataFolder.path)
        info("****************************************************")
        info("\tLoading ZeroAFK...")
        info("\tZeroAFK version: 1.0-SNAPSHOT")
        info("\tServer version: $version\n\n")

        info("\tSearching for PlaceholderAPI...")
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            error("\tCould not find PlaceholderAPI! This plugin is required.")
            info("\tAborting plugin loading...")
            Bukkit.getPluginManager().disablePlugin(this);
        }

        info("\tLoading config...")
        // Load configuration file
        saveDefaultConfig()

        info("\tLoading database configurations...")
        val host = config.getString("database.host")
        var port = config.getInt("database.port")
        val username = config.getString("database.user")
        val password = config.getString("database.password")
        val databaseName = config.getString("database.database")

        if(port == 0)
            port = 3306

        if(host.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty() || databaseName.isNullOrEmpty()) {
            info("\tZeroAFK hub is not configured. Please check your config file.")
            info("\tNo database configurations were loaded.")
            info("**************************************")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        info("\tLoading database...")
        database = ZeroDatabase(
            connection = null,
            host = host,
            port = port,
            user = username,
            password = password,
            database = databaseName
        )
        info("\tInitialising database...")
        val initialized = database.init()
        if(!initialized) {
            error("\tFailed to initialize database")
            error("\tPlease check your config file for any errors.")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }else info("\tSuccessfully initialised database...")

        info("\tLoading channels...")
        // registering channels
        api = BungeeChannelApi.of(this)
        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        this.server.messenger.registerIncomingPluginChannel(this, "BungeeCord", this)

        info("\tLoading messages...")
        // Loading messages
        val messages = JsonFile(dataFolder.path, "messages")
        if(!messages.exists())
            createMessagesDefault(messages)
        messages.reload()
        Zeroafk.messages = messages

        info("\tRegistering events...")
        // register listeners
        Bukkit.getPluginManager().registerEvents(PlayerEvents(), this)

        info("\tRegistering commands...")
        // Register commands
        getCommand("debug")?.setExecutor(Debug())
        val afkCmd = AFKCommand(
            "afk",
            "Ti manda AFK",
            "/afk",
            "",
            hidden = false,
            requiredOP = false
        )
        afkCmd.register()

        info("\tChecking for afk privileges...")
        // Loading afk options
        if(config.getBoolean("afk-options.active")) {
            info("\tFound afk options, activating...")
            // Starting to check for afk players
            checkAfkPlayers()
        }

        info("\tRegistering placeholders...")
        ZeroPlaceholder().register()

        info("\tRequesting server name...")
        // specific channel
        if(Bukkit.getOnlinePlayers().isNotEmpty())
            api.server.whenComplete { name, error ->
                serverName = name
                info("\tGot server name: $name")
            }
        else info("\tRequest aborted, no online players")

        info("\tPlugin loaded!  ")
        info("**********************************************")

        api.registerForwardListener("Afk") { _, _, message ->
            info("Received a message $message")
        }
    }

    private fun checkAfkPlayers() {
        object : BukkitRunnable() {
            override fun run() {
                playerMovements.forEach {(k,v) ->
                    val movementTimeElapsed = System.currentTimeMillis() - (v["movement"] ?: 0)
                    val chatTimeElapsed = System.currentTimeMillis() - (v["chat"] ?: 0)
                    if(
                        movementTimeElapsed > (config.getInt("afk-options.max-afk-time")*1000).toLong() &&
                        chatTimeElapsed > (config.getInt("afk-options.max-afk-time")*1000).toLong()
                    ) {
                        val player = Bukkit.getPlayer(k)
                        if(player == null) {
                            info("BungeeCord: Player $k not found")
                            playerMovements.remove(k)
                            return@forEach
                        }
                        info("Player afk: ${player.name}")
                        connectPlayerToHub(player = player)
                    }
                }
            }
        }.runTaskTimer(this, 1L, 1L)
    }

    fun connectPlayerToHub(player: Player) {
        object : BukkitRunnable() {
            override fun run() {
                info("Connecting player ${player.name} to hub")
                api.connect(player, config.getString("afk-options.hub-server-name"))
                api.forwardToPlayer(player.name, "Server", serverName.toByteArray())
                api.forward(config.getString("afk-options.hub-server-name"), "Server", serverName.toByteArray())
            }
        }.run()
    }

    private fun createMessagesDefault(jsonFile: JsonFile) {
        info("\tCreating default messages...")
        jsonFile.create()
        val messages = hashMapOf<String, Any>(
            "commands" to hashMapOf<String, String>(
                "not allowed" to "%PREFIX% &cNon sei autorizzato a usare questo comando.",
                "not player" to "%PREFIX% &cDevi essere player per eseguire questo comando.",
            ),
            "afk messages" to hashMapOf<String, String>(
                "afk" to "Il player %PLAYER% è andato nel mondo dell'AFK",
                "afk staff" to "Lo staffer %PLAYER% è andato AFK!",
                "afk return" to "%PLAYER% è tornato dal mondo degli AFK",
                "afk notify" to "%PLAYER% è andato AFK",
                "afk notify staff" to "%PLAYER% è andato AFK, per inattività"
            )
        )

        jsonFile.putAll(messages)
        jsonFile.save()
    }

    override fun onDisable() {
        info("*****************************")
        info("\tSaving logs...")
        // Plugin shutdown logic
        FileLogger.saveLogFile()
        info("\tUnregistering channels...")
        //make sure to unregister the registered channels in case of a reload
        this.server.messenger.unregisterOutgoingPluginChannel(this)
        this.server.messenger.unregisterIncomingPluginChannel(this)
        info("\t Plugin unloaded.")
        info("******************************")
    }

    private fun sendToBungeeCord(p: Player, channel: String, sub: String) {
        val b = ByteArrayOutputStream()
        val out = DataOutputStream(b)
        try {
            out.writeUTF(channel)
            out.writeUTF(sub)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        p.sendPluginMessage(this, "BungeeCord", b.toByteArray())
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != "BungeeCord") {
            return
        }
        val `in` = ByteStreams.newDataInput(message)
        val s = `in`.readUTF()
    }
}