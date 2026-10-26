package com.ven.assistsxkit.plugin

import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.model.isRemote
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.UUID

/** Owns the single active plugin session across Activity, overlay, and MCP launchers. */
object PluginRuntimeManager {

    const val EXTRA_PLUGIN_SESSION_ID = "plugin_session_id"

    enum class Status { STARTING, RUNNING }
    enum class Mode { ACTIVITY, OVERLAY }

    data class Session(
        val id: String,
        val plugin: Plugin,
        val mode: Mode,
        val status: Status,
    )

    sealed interface Reservation {
        data class Acquired(val session: Session) : Reservation
        data class Existing(val session: Session) : Reservation
        data class Busy(val session: Session) : Reservation
    }

    sealed interface StopResult {
        data object Stopped : StopResult
        data object NotRunning : StopResult
        data class Mismatch(val session: Session) : StopResult
        data class Unavailable(val session: Session) : StopResult
    }

    @Volatile
    private var activeSession: Session? = null
    private var stopAction: (() -> Unit)? = null
    private val pluginListReadiness = mutableMapOf<String, CompletableDeferred<Boolean>>()

    @Synchronized
    fun reserve(plugin: Plugin): Reservation {
        val current = activeSession
        if (current != null) {
            return if (samePlugin(current.plugin, plugin)) {
                Reservation.Existing(current)
            } else {
                Reservation.Busy(current)
            }
        }
        val session = Session(
            id = UUID.randomUUID().toString(),
            plugin = plugin,
            mode = if (plugin.indexInOverlay == true || plugin.isShowOverlay) Mode.OVERLAY else Mode.ACTIVITY,
            status = Status.STARTING,
        )
        activeSession = session
        return Reservation.Acquired(session)
    }

    fun current(): Session? = activeSession

    @Synchronized
    fun isCurrent(sessionId: String): Boolean = activeSession?.id == sessionId

    @Synchronized
    fun preparePluginListNavigation(sessionId: String): Deferred<Boolean> {
        return pluginListReadiness.getOrPut(sessionId) { CompletableDeferred() }
    }

    @Synchronized
    fun confirmPluginListVisible(sessionId: String) {
        if (activeSession?.id == sessionId) pluginListReadiness[sessionId]?.complete(true)
    }

    @Synchronized
    fun forgetPluginListNavigation(sessionId: String) {
        pluginListReadiness.remove(sessionId)
    }

    @Synchronized
    fun markRunning(sessionId: String) {
        val current = activeSession ?: return
        if (current.id == sessionId && current.status != Status.RUNNING) {
            activeSession = current.copy(status = Status.RUNNING)
        }
    }

    /** Clears only the caller's session, so a delayed old-container callback cannot clear a new run. */
    @Synchronized
    fun clear(sessionId: String): Boolean {
        if (activeSession?.id != sessionId) return false
        activeSession = null
        stopAction = null
        pluginListReadiness.remove(sessionId)?.complete(false)
        return true
    }

    @Synchronized
    fun bindStopAction(sessionId: String, action: () -> Unit) {
        if (activeSession?.id == sessionId) stopAction = action
    }

    @Synchronized
    fun stop(packageName: String): StopResult {
        val current = activeSession ?: return StopResult.NotRunning
        if (current.plugin.packageName != packageName) return StopResult.Mismatch(current)
        val action = stopAction ?: return StopResult.Unavailable(current)
        action()
        return StopResult.Stopped
    }

    fun samePlugin(first: Plugin, second: Plugin): Boolean {
        if (first.packageName.isNotBlank() && second.packageName.isNotBlank()) {
            return first.packageName == second.packageName
        }
        return first.id.isNotBlank() && first.id == second.id ||
            first.path == second.path && first.name == second.name
    }
}
