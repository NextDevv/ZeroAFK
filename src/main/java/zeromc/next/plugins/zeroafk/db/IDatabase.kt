package zeromc.next.plugins.zeroafk.db

import zeromc.next.plugins.zeroafk.Zeroafk.Companion.PLUGIN
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Time

sealed interface IDatabase {
    var connection: Connection?
    var host: String
    var port: Int
    var user: String
    var password: String
    var database: String

    fun init():Boolean{
        // Create the connection
        val url = "jdbc:mysql://${host}:${port}/$database"
        try {
            connection = DriverManager.getConnection(url, user, password)
        }catch (e: Exception) {
            PLUGIN.trace("Failed to connect to database: $database", e)
            PLUGIN.error("Possible cause: \n\t- Invalid database name\n\t- Invalid user name\n\t- Invalid password\n\t- Database doesn't exist\n\t- Database offline")
            return false
        }

        val createTableSQL = "CREATE TABLE IF NOT EXISTS zeroafk(uuid VARCHAR(36) NOT NULL PRIMARY KEY, afk_time LONG NOT NULL)"
        connection.use { conn ->
            conn!!.createStatement().use { stmt ->
                stmt.execute(createTableSQL)
                PLUGIN.info("Initialized table 'zeroafk' in database $database")
                stmt.close()
            }
        }
        return true
    }

    operator fun get(key: String): Long?
    operator fun set(key: String, value: Long): Long?
    operator fun minus(key: String) {
        // Remove a key from the database
        return
    }

    fun close() : Boolean{
        // close database
        return if (connection != null) {
            connection!!.close()
            true
        }else false
    }
}