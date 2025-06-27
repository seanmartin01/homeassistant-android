package io.homeassistant.companion.android.vehicle

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.HomeAssistantApplication
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.domain
import io.homeassistant.companion.android.common.data.prefs.impl.entities.TemplateTileConfig
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.data.websocket.WebSocketState
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.DeviceRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.EntityRegistryResponse
import io.homeassistant.companion.android.common.sensors.SensorManager
import io.homeassistant.companion.android.database.sensor.SensorDao
import io.homeassistant.companion.android.database.wear.CameraTile
import io.homeassistant.companion.android.database.wear.CameraTileDao
import io.homeassistant.companion.android.database.wear.FavoriteCaches
import io.homeassistant.companion.android.database.wear.FavoriteCachesDao
import io.homeassistant.companion.android.database.wear.FavoritesDao
import io.homeassistant.companion.android.database.wear.ThermostatTile
import io.homeassistant.companion.android.database.wear.ThermostatTileDao
import io.homeassistant.companion.android.database.wear.getAll
import io.homeassistant.companion.android.database.wear.getAllFlow
import io.homeassistant.companion.android.sensors.SensorReceiver
import io.homeassistant.companion.android.util.RegistriesDataHandler
import io.homeassistant.companion.android.util.throttleLatest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class VehicleFavoritesPanoViewModel @Inject constructor(
    private val favoritesDao: FavoritesDao,
    private val favoriteCachesDao: FavoriteCachesDao,
    private val sensorsDao: SensorDao,
    private val cameraTileDao: CameraTileDao,
    private val thermostatTileDao: ThermostatTileDao,
    private val serverManager: ServerManager,
    application: Application
) : AndroidViewModel(application) {

    enum class LoadingState {
        LOADING,
        READY,
        ERROR
    }

    private var areaRegistry: List<AreaRegistryResponse>? = null
    private var deviceRegistry: List<DeviceRegistryResponse>? = null
    private var entityRegistry: List<EntityRegistryResponse>? = null

    // TODO: This is bad, do this instead: https://stackoverflow.com/questions/46283981/android-viewmodel-additional-arguments
    fun init() {
        loadSettings()
        loadEntities()
    }

    // entities
    var entities = mutableStateMapOf<String, Entity<*>>()
        private set

    private val _supportedEntities = MutableStateFlow(emptyList<String>())
    val supportedEntities = _supportedEntities.asStateFlow()

    /**
     * IDs of favorites in the Favorites database.
     */
    val favoriteEntityIds = favoritesDao.getAllFlow().collectAsState()
    private val favoriteCaches = favoriteCachesDao.getAll()

    val cameraTiles = cameraTileDao.getAllFlow().collectAsState()
    var cameraEntitiesMap = mutableStateMapOf<String, SnapshotStateList<Entity<*>>>()
        private set

    val thermostatTiles = thermostatTileDao.getAllFlow().collectAsState()
    var climateEntitiesMap = mutableStateMapOf<String, SnapshotStateList<Entity<*>>>()
        private set

    var areas = mutableListOf<AreaRegistryResponse>()
        private set

    var entitiesByArea = mutableStateMapOf<String, SnapshotStateList<Entity<*>>>()
        private set
    var entitiesByDomain = mutableStateMapOf<String, SnapshotStateList<Entity<*>>>()
        private set
    var entitiesByAreaOrder = mutableStateListOf<String>()
        private set
    var entitiesByDomainOrder = mutableStateListOf<String>()
        private set

    // Content of EntityListView
    var entityLists = mutableStateMapOf<String, List<Entity<*>>>()
    var entityListsOrder = mutableStateListOf<String>()
    var entityListFilter: (Entity<*>) -> Boolean = { true }

    // settings
    var loadingState = mutableStateOf(LoadingState.LOADING)
        private set
    var isToastEnabled = mutableStateOf(false)
        private set
    var isShowShortcutTextEnabled = mutableStateOf(false)
        private set

    companion object {
        val domainsWithNames = mapOf(
            "button" to R.string.buttons,
            "cover" to R.string.covers,
            "fan" to R.string.fans,
            "input_boolean" to R.string.input_booleans,
            "input_button" to R.string.input_buttons,
            "light" to R.string.lights,
            "lock" to R.string.locks,
            "switch" to R.string.switches,
            "script" to R.string.scripts,
            "scene" to R.string.scenes
        )
        val supportedDomains = domainsWithNames.keys.toList()
    }

    fun stringForDomain(domain: String): String? =
        domainsWithNames[domain]?.let { getApplication<Application>().getString(it) }

    val sensors = sensorsDao.getAllFlow().collectAsState()

    var availableSensors = emptyList<SensorManager.BasicSensor>()

    private fun loadSettings() {
        viewModelScope.launch {
            if (!serverManager.isRegistered()) {
                return@launch
            }
            isToastEnabled.value = true
            isShowShortcutTextEnabled.value = true

            val assistantAppComponent = ComponentName(
                BuildConfig.APPLICATION_ID,
                "io.homeassistant.companion.android.conversation.AssistantActivity"
            )
        }
    }

    fun loadEntities() {
        if (!serverManager.isRegistered()) return
        viewModelScope.launch {
            try {
                // Load initial state
                loadingState.value = LoadingState.LOADING
                updateUI()

                // Finished initial load, update state
                val webSocketState = serverManager.webSocketRepository().getConnectionState()
                if (webSocketState == WebSocketState.CLOSED_AUTH) {
                    return@launch
                }
                loadingState.value = if (webSocketState == WebSocketState.ACTIVE) {
                    LoadingState.READY
                } else {
                    LoadingState.ERROR
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading entities")
                loadingState.value = LoadingState.ERROR
            }
        }
    }

    private fun updateEntityStates(entity: Entity<*>) {
        if (supportedDomains.contains(entity.domain)) {
            entities[entity.entityId] = entity
            // add to cache if part of favorites
            if (favoriteEntityIds.value.contains(entity.entityId)) {
                addCachedFavorite(entity.entityId)
            }
        }
    }

    suspend fun updateUI() = withContext(Dispatchers.IO) {
        if (!serverManager.isRegistered()) return@withContext
        val getEntityRegistry = async { serverManager.webSocketRepository().getEntityRegistry() }
        val getEntities = async { serverManager.integrationRepository().getEntities() }

        entityRegistry = getEntityRegistry.await()

        _supportedEntities.value = getSupportedEntities()

        getEntities.await()?.also {
            entities.clear()
            it.forEach { state -> updateEntityStates(state) }

            // Special lists: camera entities and climate entities
            val cameraEntities = it.filter { entity -> entity.domain == "camera" }
            cameraEntitiesMap["camera"] = mutableStateListOf<Entity<*>>().apply { addAll(cameraEntities) }
            val climateEntities = it.filter { entity -> entity.domain == "climate" }
            climateEntitiesMap["climate"] = mutableStateListOf<Entity<*>>().apply { addAll(climateEntities) }
        }
    }

    suspend fun entityUpdates() {
        if (!serverManager.isRegistered()) {
            return
        }
        serverManager.integrationRepository().getEntityUpdates(supportedEntities.value)?.collect {
            updateEntityStates(it)
        }
    }

    suspend fun entityRegistryUpdates() {
        if (!serverManager.isRegistered()) {
            return
        }
        serverManager.webSocketRepository().getEntityRegistryUpdates()?.throttleLatest(1000)?.collect {
            entityRegistry = serverManager.webSocketRepository().getEntityRegistry()
            _supportedEntities.value = getSupportedEntities()
            updateEntityDomains()
        }
    }

    private fun getSupportedEntities(): List<String> =
        entityRegistry
            .orEmpty()
            .map { it.entityId }
            .filter { it.split(".")[0] in supportedDomains }

    private fun updateEntityDomains() {
        val entitiesList = entities.values.toList().sortedBy { it.entityId }
        val areasList = areaRegistry.orEmpty().sortedBy { it.name }
        val domainsList = entitiesList.map { it.domain }.distinct()

        // Create a list with all areas + their entities
        areasList.forEach { area ->
            val entitiesInArea = mutableStateListOf<Entity<*>>()
            entitiesInArea.addAll(
                entitiesList
                    .filter { getAreaForEntity(it.entityId)?.areaId == area.areaId }
                    .map { it as Entity<Map<String, Any>> }
                    .sortedBy { (it.attributes["friendly_name"] ?: it.entityId) as String }
            )
            entitiesByArea[area.areaId]?.let {
                it.clear()
                it.addAll(entitiesInArea)
            } ?: run {
                entitiesByArea[area.areaId] = entitiesInArea
            }
        }
        entitiesByAreaOrder.clear()
        entitiesByAreaOrder.addAll(areasList.map { it.areaId })
        // Quick check: are there any areas in the list that no longer exist?
        entitiesByArea.forEach {
            if (!areasList.any { item -> item.areaId == it.key }) {
                entitiesByArea.remove(it.key)
            }
        }

        // Create a list with all discovered domains + their entities
        domainsList.forEach { domain ->
            val entitiesInDomain = mutableStateListOf<Entity<*>>()
            entitiesInDomain.addAll(entitiesList.filter { it.domain == domain })
            entitiesByDomain[domain]?.let {
                it.clear()
                it.addAll(entitiesInDomain)
            } ?: run {
                entitiesByDomain[domain] = entitiesInDomain
            }
        }
        entitiesByDomainOrder.clear()
        entitiesByDomainOrder.addAll(domainsList)
    }

    fun enableDisableSensor(sensorManager: SensorManager, sensorId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val basicSensor = sensorManager.getAvailableSensors(getApplication())
                .first { basicSensor -> basicSensor.id == sensorId }
            updateSensorEntity(sensorsDao, basicSensor, isEnabled)

            if (isEnabled) {
                try {
                    sensorManager.requestSensorUpdate(getApplication())
                } catch (e: Exception) {
                    Timber.e(e, "Exception while requesting update for sensor $sensorId")
                }
            }
        }
    }

    private suspend fun updateSensorEntity(
        sensorDao: SensorDao,
        basicSensor: SensorManager.BasicSensor,
        isEnabled: Boolean
    ) {
        serverManager.getServer()?.id?.let { serverId ->
            sensorDao.setSensorsEnabled(listOf(basicSensor.id), serverId, isEnabled)
            SensorReceiver.updateAllSensors(getApplication())
        }
    }

    fun updateAllSensors(sensorManager: SensorManager) {
        availableSensors = emptyList()
        viewModelScope.launch {
            val context = getApplication<HomeAssistantApplication>().applicationContext
            availableSensors = sensorManager
                .getAvailableSensors(context)
                .sortedBy { context.getString(it.name) }.distinct()
        }
    }

    fun initAllSensors() {
        viewModelScope.launch {
            for (manager in SensorReceiver.MANAGERS) {
                for (basicSensor in manager.getAvailableSensors(getApplication())) {
                    manager.isEnabled(getApplication(), basicSensor)
                }
            }
        }
    }

    fun getAreaForEntity(entityId: String): AreaRegistryResponse? =
        RegistriesDataHandler.getAreaForEntity(entityId, areaRegistry, deviceRegistry, entityRegistry)

    fun getCategoryForEntity(entityId: String): String? =
        RegistriesDataHandler.getCategoryForEntity(entityId, entityRegistry)

    fun getHiddenByForEntity(entityId: String): String? =
        RegistriesDataHandler.getHiddenByForEntity(entityId, entityRegistry)


    fun setCameraTileEntity(tileId: Int, entityId: String) = viewModelScope.launch {
        val current = cameraTileDao.get(tileId)
        val updated = current?.copy(entityId = entityId) ?: CameraTile(id = tileId, entityId = entityId)
        cameraTileDao.add(updated)
    }

    fun setCameraTileRefreshInterval(tileId: Int, interval: Long) = viewModelScope.launch {
        val current = cameraTileDao.get(tileId)
        val updated = current?.copy(refreshInterval = interval) ?: CameraTile(id = tileId, refreshInterval = interval)
        cameraTileDao.add(updated)
    }

    fun setThermostatTileEntity(tileId: Int, entityId: String) = viewModelScope.launch {
        val current = thermostatTileDao.get(tileId)
        val updated = current?.copy(entityId = entityId) ?: ThermostatTile(id = tileId, entityId = entityId)
        thermostatTileDao.add(updated)
    }

    fun setThermostatTileRefreshInterval(tileId: Int, interval: Long) = viewModelScope.launch {
        val current = thermostatTileDao.get(tileId)
        val updated = current?.copy(refreshInterval = interval) ?: ThermostatTile(id = tileId, refreshInterval = interval)
        thermostatTileDao.add(updated)
    }

    fun setThermostatTileShowName(tileId: Int, showName: Boolean) = viewModelScope.launch {
        val current = thermostatTileDao.get(tileId)
        val updated = current?.copy(showEntityName = showName) ?: ThermostatTile(id = tileId, showEntityName = showName)
        thermostatTileDao.add(updated)
    }

    fun addFavoriteEntity(entityId: String) {
        viewModelScope.launch {
            favoritesDao.addToEnd(entityId)
            addCachedFavorite(entityId)
        }
    }

    fun removeFavoriteEntity(entityId: String) {
        viewModelScope.launch {
            favoritesDao.delete(entityId)
            favoriteCachesDao.delete(entityId)
        }
    }

    fun getCachedEntity(entityId: String): FavoriteCaches? =
        favoriteCaches.find { it.id == entityId }

    private fun addCachedFavorite(entityId: String) {
        viewModelScope.launch {
            val entity = entities[entityId]
            val attributes = entity?.attributes as Map<*, *>
            val icon = attributes["icon"] as String?
            val name = attributes["friendly_name"]?.toString() ?: entityId
            favoriteCachesDao.add(FavoriteCaches(entityId, name, icon))
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            favoriteCachesDao.deleteAll()
        }
    }

    /**
     * Convert a Flow into a State object that updates until the view model is cleared.
     */
    private fun <T> Flow<T>.collectAsState(
        initial: T
    ): State<T> {
        val state = mutableStateOf(initial)
        viewModelScope.launch {
            collect { state.value = it }
        }
        return state
    }
    private fun <T> Flow<List<T>>.collectAsState(): State<List<T>> = collectAsState(initial = emptyList())
}
