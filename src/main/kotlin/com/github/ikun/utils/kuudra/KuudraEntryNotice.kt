package com.github.ikun.utils.kuudra

class KuudraEntryNotice {
    private var enteredWorld: Any? = null

    fun observe(message: String, playerName: String, world: Any): Boolean {
        val lines = message.replace(Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE), "")
            .replace("\\n", "\n").trim().lines().map { it.trim() }
        val text = when {
            lines.size == 1 -> lines.single()
            lines.size == 3 && lines.first().matches(Regex("-{3,}")) && lines.last().matches(Regex("-{3,}")) -> lines[1]
            else -> return false
        }
        val name = entry.matchEntire(text)?.groupValues?.get(1) ?: return false
        if (!name.equals(playerName, ignoreCase = true)) return false
        enteredWorld = world
        return true
    }

    fun matches(world: Any) = enteredWorld === world

    fun reset() {
        enteredWorld = null
    }

    companion object {
        private val entry = Regex("^(?:\\[[^]]+]\\s+)?([A-Za-z0-9_]{1,16}) entered Kuudra['’]s Hollow, (?:Basic|Hot|Burning|Fiery|Infernal) Tier!$")
    }
}
