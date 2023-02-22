package zeromc.next.plugins.zeroafk.logger

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

class FileLogger {
    companion object {
        private var PATH = "n/a"

        fun setPath(path: String) {
            PATH = path
        }

        fun getPath(): String {
            return PATH
        }

        fun logIntoFile(level: LogLevel, message: String, t: Throwable? = null) {
            val logFolder = File(PATH)
            if (!logFolder.exists()) {
                logFolder.mkdir()
            }
            val logFile = File(logFolder, "latest.log")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            // Formatted date and time
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())

            if(t != null) {
                logFile.appendText("[$date] [${LogLevel.TRACE}] AN ERROR OCCURRED \n\t HERES AND WALKTHROUGH: ")
                logFile.appendText("\n\tMessage: ${t.message}")
                logFile.appendText("\n\tCause: ${t.cause}")
                logFile.appendText("\n\tPlugin message: $message")
            }else logFile.appendText("[${date}] [${level.name}] $message\n")
        }

        fun saveLogFile():Boolean {
            val logFolder = File(PATH)
            if (!logFolder.exists()) {
                logFolder.mkdir()
            }
            val logFile = File(logFolder, "latest.log")
            if (!logFile.exists())
                return false
            logIntoFile(LogLevel.INFO, "--- END OF LOG STREAM ---")
            // Rename the file to the current date and time
            val date = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Date())
            logFile.renameTo(File(logFolder, "$date.log"))
            return true
        }
    }
}