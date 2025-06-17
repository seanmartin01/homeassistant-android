package io.homeassistant.companion.android.vehicle

import android.content.pm.PackageManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.ListItem
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.ListHeader
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.integration.impl.entities.EntityResponse
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.util.compose.HomeAssistantAppTheme
import io.homeassistant.companion.android.util.previewEntity1
import io.homeassistant.companion.android.util.previewEntity2
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.floor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class VehicleFavoritesPanoActivity : ComponentActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    private val viewModel: VehicleFavoritesPanoViewModel by viewModels{ VehicleFavoritesPanoViewModelFactory()}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeAssistantAppTheme {
                VehicleFavoritesPanoScreen(
                    entityLists = viewModel.entityLists,
                    entityListsOrder = viewModel.entityListsOrder,
                    entityListFilter = viewModel.entityListFilter,
                    isToastEnabled = viewModel.isToastEnabled.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.title =
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
                getString(commonR.string.android_automotive_favorites)
            } else {
                getString(commonR.string.aa_favorites)
            }
    }
}

@Composable
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
fun VehicleFavoritesPanoScreen(
    entityLists: Map<String, List<Entity<*>>>,
    entityListsOrder: List<String>,
    entityListFilter: (Entity<*>) -> Boolean,
    isToastEnabled: Boolean
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Home Assistant Favorites") }) }
    ) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            for (header in entityListsOrder) {
                val entities = entityLists[header].orEmpty()
                if (entities.isNotEmpty()) {
                    item {
                        if (entityLists.size > 1) {
                            ListHeader() {
                                Row {
                                    Text(
                                        text = header
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        } else {
                            ListHeader {
                                val maxLines = with(LocalDensity.current) {
                                    if (LocalTextStyle.current.fontSize.isSp) {
                                        floor(48 / LocalTextStyle.current.fontSize.toDp().value).toInt() // A ListHeader is 48dp
                                    } else {
                                        1 // Fallback as em cannot be converted
                                    }
                                }
                                Text(
                                    text = header,
                                    modifier = Modifier,
                                    textAlign = TextAlign.Center,
                                    maxLines = maxLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    val filtered = entities.filter { entityListFilter(it) }
                    items(filtered, key = { it.entityId }) { entity ->
                        EntityIndicator(
                            entity,
                            { },
                            false,
                            isToastEnabled,
                        )
                    }

                    if (filtered.isEmpty()) {
                        item {
                            androidx.compose.material.CircularProgressIndicator(
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewVehicleFavoritesPanoScreen() {
    VehicleFavoritesPanoScreen(
        entityLists = mapOf(stringResource(commonR.string.lights) to listOf(previewEntity1, previewEntity2)),
        entityListsOrder = listOf(stringResource(commonR.string.lights)),
        entityListFilter = { true },
        isToastEnabled = false
    )
}
