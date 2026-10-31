package com.github.ikun.utils

import com.github.ikun.Ikun.mc
import com.github.ikun.event.EventBus.register
import com.github.ikun.event.impl.ChatMessageEvent
import com.github.ikun.event.priority.EventPriority
import com.github.ikun.utils.location.LocationUtils

/**
 * @author Odin
 * @link https://github.com/odtheking/OdinFabric/blob/main/src/main/kotlin/com/odtheking/odin/utils/skyblock/PartyUtils.kt
 * Modified
 */
object PartyUtils {
    private val state = PartyState()
    val members get() = state.members
    val partyLeader get() = state.partyLeader
    val isInParty get() = state.isInParty
    val sessionId get() = state.sessionId

    fun init() {
        register<ChatMessageEvent>(EventPriority.HIGHEST) {
            if (!LocationUtils.onHypixel) return@register
            state.accept(event.unformattedText, mc.user.name)
        }
    }

    fun isLeader(): Boolean = partyLeader?.equals(mc.user.name, true) == true
    fun reset() = state.reset()
}

class PartyState {
    private val joinedSelf = Regex("^You have joined ((?:\\[[^]]*?])? ?)?(\\w{1,16})'s? party!$")
    private val joinedOther = Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16}) joined the party\\.$")
    private val leftParty = Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16}) has left the party\\.$")
    private val kickedParty = Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16}) has been removed from the party\\.$")
    private val kickedOffline = Regex("^Kicked ((?:\\[[^]]*?])? ?)?(\\w{1,16}) because they were offline\\.$")
    private val kickedDisconnected = Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16}) was removed from your party because they disconnected\\.$")
    private val transferLeave = Regex("^The party was transferred to ((?:\\[[^]]*?])? ?)?(\\w{1,16}) because ((?:\\[[^]]*?])? ?)?(\\w{1,16}) left$")
    private val transferBy = Regex("^The party was transferred to ((?:\\[[^]]*?])? ?)?(\\w{1,16}) by ((?:\\[[^]]*?])? ?)?(\\w{1,16})$")
    private val partyChat = Regex("^Party > ((?:\\[[^]]*?])? ?)?(\\w{1,16}): (.+)$")
    private val partyInvite = Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16}) invited ((?:\\[[^]]*?])? ?)?(\\w{1,16}) to the party! They have 60 seconds to accept.$")
    private val leaderDisconnected = Regex("^The party leader, ((?:\\[[^]]*?])? ?)?(\\w{1,16}) has disconnected, they have 5 minutes to rejoin before the party is disbanded\\.$")
    private val leaderRejoined = Regex("^The party leader ((?:\\[[^]]*?])? ?)?(\\w{1,16}) has rejoined\\.$")
    private val memberFormat = Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16})$")
    private val partyWith = Regex("^You'll be partying with: (.+)$")

    private val queuedInFinder = Regex("^Party Finder > Your (?:party|kuudra group) has been queued(?: in the (?:party|dungeon|kuudra) finder)?!$")
    private val dungeonJoin = Regex("^Party Finder > (\\w{1,16}) joined the dungeon group! \\((\\w+) Level (\\d+)\\)$")
    private val kuudraJoin = Regex("^Party Finder > ((?:\\[[^]]*?])? ?)?(\\w{1,16}) joined the group! \\(Combat Level (\\d+)\\)$")
    private val membersList = Regex("^Party (Leader|Moderators|Members): (.+)$")

    private val disbandPatterns = listOf(
        Regex("^((?:\\[[^]]*?])? ?)?(\\w{1,16}) has disbanded the party!$"),
        Regex("^You have been kicked from the party by ((?:\\[[^]]*?])? ?)?(\\w{1,16})$"),
        Regex("^The party was disbanded because all invites expired and the party was empty.$"),
        Regex("^The party was disbanded because the party leader disconnected.$"),
        Regex("^You left the party.$"),
        Regex("^You are not currently in a party.$")
    )

    val members = mutableListOf<String>()

    var sessionId = 0L
        private set

    var partyLeader: String? = null
        private set

    var isInParty: Boolean = false
        private set

    fun accept(message: String, ownName: String) {
        joinedOther.find(message)?.let { return addMember(it.groupValues[2]) }

        joinedSelf.find(message)?.let {
            disband()
            addMember(it.groupValues[2])
            partyLeader = it.groupValues[2]
            addMember(ownName)
            return
        }

        leftParty.find(message)?.let { return removeMember(it.groupValues[2]) }

        kickedParty.find(message)?.let { return removeMember(it.groupValues[2]) }

        kickedOffline.find(message)?.let { return removeMember(it.groupValues[2]) }

        kickedDisconnected.find(message)?.let { return removeMember(it.groupValues[2]) }

        transferBy.find(message)?.let {
            addMember(it.groupValues[2])
            addMember(it.groupValues[4])
            partyLeader = it.groupValues[2]
            return
        }

        transferLeave.find(message)?.let {
            if (it.groupValues[4].equals(ownName, ignoreCase = true)) {
                disband()
                return
            }
            addMember(it.groupValues[2])
            partyLeader = it.groupValues[2]
            removeMember(it.groupValues[4])
            return
        }

        leaderDisconnected.find(message)?.let {
            partyLeader = it.groupValues[2]
            return
        }

        leaderRejoined.find(message)?.let {
            partyLeader = it.groupValues[2]
            return
        }

        partyChat.find(message)?.let {
            addMember(it.groupValues[2])
            return
        }

        partyInvite.find(message)?.let {
            addMember(it.groupValues[2])
            if (partyLeader == null) partyLeader = it.groupValues[2]
            return
        }

        queuedInFinder.find(message)?.let {
            addMember(ownName)
            if (partyLeader == null) partyLeader = ownName
            return
        }

        for (pattern in disbandPatterns) {
            if (pattern.containsMatchIn(message)) return disband()
        }

        membersList.find(message)?.let { match ->
            val type = match.groupValues[1]

            match.groupValues[2].split(" ●").forEach { segment ->
                val memberMatch = memberFormat.find(segment.trim()) ?: return@forEach
                addMember(memberMatch.groupValues[2])
                if (type == "Leader") partyLeader = memberMatch.groupValues[2]
            }
            return
        }

        partyWith.find(message)?.let { match ->
            match.groupValues[1].split(", ").forEach { playerName ->
                val memberMatch = memberFormat.find(playerName.trim()) ?: return@forEach
                addMember(memberMatch.groupValues[2])
            }
            return
        }


        kuudraJoin.find(message)?.let { return addMember(it.groupValues[2]) }

        dungeonJoin.find(message)?.let { return addMember(it.groupValues[1]) }
    }

    fun reset() = disband()

    private fun addMember(playerName: String) {
        if (! isInParty) isInParty = true
        if (playerName !in members) members.add(playerName)
    }

    private fun removeMember(playerName: String) {
        if (playerName !in members) return
        members.remove(playerName)
        if (members.isEmpty()) disband()
    }

    private fun disband() {
        sessionId++
        members.clear()
        partyLeader = null
        isInParty = false
    }
}
