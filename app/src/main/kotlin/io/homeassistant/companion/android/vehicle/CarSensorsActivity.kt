package io.homeassistant.companion.android.vehicle

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.integration.impl.entities.EntityResponse
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.server.Server
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class CarSensorsViewModel @Inject constructor(
    private val serverManager: ServerManager
) : ViewModel() {
    var carSensors by mutableStateOf<List<Entity<Any>>?>(null)
    var error by mutableStateOf<String?>(null)

    fun loadSensors() {
        viewModelScope.launch {
            val serverId = serverManager.getServer()?.id
            if (serverId == null) {
                error = "No server available"
                Timber.e("No server available for Car Pano Activity")
                return@launch
            }
            val allEntities = serverManager.integrationRepository(serverId).getEntities()
            carSensors = allEntities?.filter { entity ->
                val attributes = entity.attributes as Map<String, Any?>
                entity.entityId.startsWith("sensor.") &&
                    (attributes["device_class"] as? String == "car" ||
                     entity.entityId.contains("car", ignoreCase = true))
            }
            if (carSensors == null) {
                error = "Failed to load car sensors"
                Timber.e("Failed to load car sensors for server ID: $serverId")
            } else if (carSensors?.isEmpty() == true) {
                error = "No car sensors found"
                Timber.e("No car sensors found for server ID: $serverId")
            } else {
                Timber.d("Car sensors loaded: ${carSensors?.size ?: 0} entities")
            }
        }
    }
}

@AndroidEntryPoint
class CarSensorsActivity : ComponentActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    private val viewModel: CarSensorsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadSensors()
        setContent {
            HomeAssistantAppTheme {
                CarSensorsScreen(viewModel)
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
fun CarSensorsScreen(viewModel: CarSensorsViewModel) {
    val carSensors = viewModel.carSensors ?: emptyList()
    val error = viewModel.error
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Car Sensors") }) }
    ) { padding ->
        when {
            error != null -> Text(
                error,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colors.error
            )
            carSensors == null -> androidx.compose.material.CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
            carSensors.isEmpty() -> Text(
                "No car sensors found",
                modifier = Modifier.padding(16.dp)
            )
            else -> LazyColumn(contentPadding = padding) {
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
}

@Preview
@Composable
private fun CarSensorsScreenPreview() {
    val fakeViewModel = object {
        val carSensors = listOf(
            Entity(
                entityId = "sensor.car_speed",
                state = "60",
                attributes = mapOf("friendlyName" to "Car Speed", "device_class" to "car"),
                lastChanged = Calendar.getInstance(),
                lastUpdated = Calendar.getInstance(),
                context = null
            ) as Entity<Any>
        )
        val error: String? = null
    }
    HomeAssistantAppTheme {
        CarSensorsScreenPreviewContent(
            carSensors = fakeViewModel.carSensors,
            error = fakeViewModel.error
        )
    }
}

@Composable
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
private fun CarSensorsScreenPreviewContent(
    carSensors: List<Entity<Any>>,
    error: String?
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Car Sensors") }) }
    ) { padding ->
        when {
            error != null -> Text(
                error,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colors.error
            )
            carSensors.isEmpty() -> Text(
                "No car sensors found",
                modifier = Modifier.padding(16.dp)
            )
            else -> LazyColumn(contentPadding = padding) {
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
}
