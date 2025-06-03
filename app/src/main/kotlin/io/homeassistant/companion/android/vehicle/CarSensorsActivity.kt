package io.homeassistant.companion.android.vehicle

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.integration.impl.entities.EntityResponse
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

@AndroidEntryPoint
class CarSensorsActivity : ComponentActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeAssistantAppTheme {
                CarSensorsScreen(serverManager, this)
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
fun CarSensorsScreen(
    serverManager: ServerManager,
    context: Context? = null) {
    var carSensors by remember { mutableStateOf<List<Entity<Any>>>(emptyList()) }
    LaunchedEffect(Unit) {
        val serverId = serverManager.getServer()?.id ?: run {
            // report error if no server is available
            context?.let {
                // Show a toast or dialog to inform the user
                // For example: Toast.makeText(it, "No server available", Toast.LENGTH_SHORT).show()
                Toast.makeText(it, "No server available", Toast.LENGTH_SHORT).show()
                Timber.e("No server available for Car Sensors")
            }
            return@LaunchedEffect
        }
        val allEntities = serverManager.integrationRepository(serverId).getEntities()
        carSensors = allEntities?.filter { entity ->
            val attributes = entity.attributes as Map<String, Any?>
            entity.entityId.startsWith("sensor.") &&
            (attributes["device_class"] as? String == "car" ||
             entity.entityId.contains("car", ignoreCase = true))
        } ?: emptyList()
        if (carSensors.isEmpty()) {
            context?.let {
                Toast.makeText(it, "No car sensors found", Toast.LENGTH_SHORT).show()
                Timber.w("No car sensors found for server ID: $serverId")
            }
        }
        Timber.d("Car sensors loaded: ${carSensors.size} entities")
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Car Sensors") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(carSensors) { entity ->
                val attributes = entity.attributes as Map<String, Any?>
                ListItem(
                    text = { Text(attributes["friendlyName"] as? String ?: entity.entityId) },
                    secondaryText = { Text(entity.state ?: "") }
                )
                Divider()
            }
        }
    }
}

@Preview
@Composable
private fun CarSensorsScreenPreview() {
    HomeAssistantAppTheme {
        CarSensorsScreen(serverManager = object : ServerManager {
                override fun isRegistered() = false
                override suspend fun addServer(server: Server) = 0
                override fun getServer(id: Int): Server? = null
                override fun getServer(webhookId: String): Server? = null
                override fun activateServer(id: Int) {}
                override fun updateServer(server: Server) {}
                override suspend fun convertTemporaryServer(id: Int): Int? = null
                override suspend fun removeServer(id: Int) {}
                override fun authenticationRepository(serverId: Int) = TODO()
                override fun integrationRepository(serverId: Int) = TODO()
                override fun webSocketRepository(serverId: Int) = TODO()
                override val defaultServers: List<Server> = emptyList()
                override val defaultServersFlow = MutableStateFlow<List<Server>>(emptyList())
            }
        )
    }
}
