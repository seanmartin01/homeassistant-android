package io.homeassistant.companion.android.vehicle

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.Text
import com.mikepenz.iconics.compose.Image
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.EntityExt
import io.homeassistant.companion.android.common.data.integration.domain
import io.homeassistant.companion.android.common.data.integration.getIcon
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.util.STATE_UNAVAILABLE
import io.homeassistant.companion.android.util.previewEntity1
import io.homeassistant.companion.android.util.previewEntity3
import io.homeassistant.companion.android.util.AutomotiveToggleChip

@Composable
fun EntityIndicator(
    entity: Entity<*>,
    isToastEnabled: Boolean
) {
    val context = LocalContext.current
    val attributes = entity.attributes as Map<*, *>
    val iconBitmap = entity.getIcon(LocalContext.current)
    val friendlyName = attributes["friendly_name"].toString()
    val nameModifier = Modifier.fillMaxWidth()
    if (entity.domain in EntityExt.DOMAINS_TOGGLE) {
        val isChecked = entity.isActive()
        val isEnabled = entity.state != STATE_UNAVAILABLE
        val colors = AutomotiveToggleChip.entityToggleChipBackgroundColors(entity, isChecked)
        ToggleChip(
            checked = isChecked,
            onCheckedChange = {},
            modifier = Modifier.fillMaxWidth(),
            appIcon = {
                Image(
                    asset = iconBitmap,
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface)
                )
            },
            label = {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.button,
                    LocalContentColor provides colors.contentColor(enabled = isEnabled, checked = isChecked).value
                ) {
                    Text(
                        text = friendlyName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = nameModifier
                    )
                }
            },
            enabled = isEnabled,
            toggleControl = { Switch(checked = isChecked) },
            colors = colors
        )
    } else {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = entity.state != STATE_UNAVAILABLE,
            onClick = {},
            colors = ButtonDefaults.primaryButtonColors(),
            content = {
                Image(
                    asset = iconBitmap,
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface)
                )
                Text(
                    text = friendlyName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = nameModifier
                )
            }
        )
    }
}

@Preview
@Composable
private fun PreviewEntityUI() {
    Column {
        EntityIndicator(
            entity = previewEntity1,
            isToastEnabled = false
        )
        EntityIndicator(
            entity = previewEntity3,
            isToastEnabled = true
        )
    }
}
