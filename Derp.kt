package rip.sunrise.server.config

import com.google.gson.Gson
import rip.sunrise.packets.clientbound.ScriptWrapper
import java.io.File

class Config(private val configFile: File) {
    var revisionData = ""
    var scripts = mutableListOf<Script>()
    var serverUrl = ""

    init {
        require(configFile.exists()) { "Configuration file does not exist: ${configFile.absolutePath}" }
        load()
    }

    fun load() {
        runCatching {
            val gson = Gson()
            val config = gson.fromJson(configFile.reader(), ConfigData::class.java)

            val revisionFile = File(config.revisionFile)
            require(revisionFile.isFile) { "Revision file ${revisionFile.absolutePath} isn't a normal file!" }
            this.revisionData = revisionFile.readText()  // Ensure reading as plain text

            val scriptConfigDirectory = File(config.scriptConfigDir)
            require(scriptConfigDirectory.isDirectory) { "Script config directory ${scriptConfigDirectory.absolutePath} is not a directory!" }

            scripts.clear()
            scriptConfigDirectory.listFiles()?.forEachIndexed { index, file ->
                if (file.isFile) {  // Only process files, skip directories
                    val scriptConfig = gson.fromJson(file.reader(), ScriptConfig::class.java)

                    val scriptJar = File(scriptConfig.jarFile)
                    require(scriptJar.isFile) { "Script jar ${scriptJar.absolutePath} isn't a normal file!" }

                    val optionFile = File(scriptConfig.optionFile)
                    require(optionFile.isFile) { "Option file ${optionFile.absolutePath} isn't a normal file!" }

                    val metadata = ScriptWrapper(
                        0,
                        scriptConfig.description,
                        scriptConfig.name,
                        0,
                        scriptConfig.version,
                        "",
                        "",
                        scriptConfig.author,
                        "",
                        scriptConfig.imageUrl,
                        index,
                        index,
                        false
                    )

                    scripts.add(Script(metadata, scriptJar.readBytes(), optionFile.readLines()))
                }
            }

            this.serverUrl = config.serverUrl
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun getScript(id: Int): Script {
        return scripts.firstOrNull { it.metadata.d == id } ?: error("Couldn't find script with id $id")
    }

    private data class ConfigData(val revisionFile: String, val scriptConfigDir: String, val serverUrl: String)
    class Script(val metadata: ScriptWrapper, val bytes: ByteArray, val options: List<String>)
}
