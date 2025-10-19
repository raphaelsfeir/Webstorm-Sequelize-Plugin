/**
 * EnvManager.kt
 * --------------
 * Provides persistent storage and access to the currently selected
 * Sequelize environment within a project (e.g., "development", "staging", "production").
 *
 * The environment is stored in the project’s configuration file
 * (`.idea/sequelize_env.xml`) using IntelliJ’s [PersistentStateComponent] API.
 * This allows the plugin to remember the last selected environment
 * between IDE sessions and project reloads.
 *
 * Typical usage:
 * ```kotlin
 * val envManager = project.getService(EnvManager::class.java)
 * val currentEnv = envManager?.get() ?: "development"
 * envManager?.set("production")
 * ```
 *
 * Responsibilities:
 *  - Persist the active Sequelize environment setting.
 *  - Provide getters and setters for accessing or updating the environment.
 *  - Integrate with the IDE’s persistent state mechanism via XML storage.
 *
 * Dependencies:
 *  - IntelliJ Platform’s [PersistentStateComponent] for automatic serialization.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of persistent environment management.
 *
 * @license
 *   MIT License
 */

package core

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service

/**
 * Project-level service responsible for persisting the active Sequelize environment.
 *
 * Uses IntelliJ’s [PersistentStateComponent] to automatically store and reload
 * the environment setting in an XML file under `.idea/sequelize_env.xml`.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "SequelizeEnvState",
    storages = [Storage("sequelize_env.xml")]
)
class EnvManager : PersistentStateComponent<EnvManager.State> {

    /**
     * Simple serializable data holder representing the environment state.
     *
     * @property environment The current Sequelize environment.
     * Defaults to `"development"`.
     */
    data class State(var environment: String = "development")

    /** Current persisted state of the environment. */
    private var state = State()

    /**
     * Returns the current persisted state for serialization.
     */
    override fun getState() = state

    /**
     * Loads the previously saved state from disk.
     *
     * @param s The deserialized [State] instance from storage.
     */
    override fun loadState(s: State) {
        state = s
    }

    /**
     * Returns the current environment string (e.g., `"development"`).
     */
    fun get(): String = state.environment

    /**
     * Updates the active environment and persists the new value.
     *
     * @param env The new environment name to store.
     */
    fun set(env: String) {
        state.environment = env
    }
}
