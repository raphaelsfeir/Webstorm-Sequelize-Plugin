package core

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
@State(name = "SequelizeEnvState", storages = [Storage("sequelize_env.xml")])
class EnvManager : PersistentStateComponent<EnvManager.State> {
    data class State(var environment: String = "development")
    private var state = State()

    override fun getState() = state
    override fun loadState(s: State) { state = s }

    fun get() = state.environment
    fun set(env: String) { state.environment = env }
}
