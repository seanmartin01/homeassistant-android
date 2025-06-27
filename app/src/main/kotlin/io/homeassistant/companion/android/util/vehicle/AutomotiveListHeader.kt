package io.homeassistant.companion.android.util.vehicle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A simple, styled header for use in lists within an Android Automotive app.
 *
 * This composable provides a visually distinct text label to group list items,
 * similar to the function of a Wear OS ListHeader, but styled appropriately for
 * the automotive environment using Material Design 2.
 *
 * @param text The string to display in the header.
 * @param modifier The modifier to be applied to the component.
 */
@Composable
fun AutomotiveListHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(), // Using uppercase is a common styling for headers
        modifier = modifier
            .fillMaxWidth()
            // Add generous padding, especially at the top, to separate sections.
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        // Use a distinct color. The primary color or a semi-transparent 'onSurface'
        // color works well.
        color = MaterialTheme.colors.primary,
        // Use a typography style that stands out. `overline` is a good choice for
        // a small, all-caps header. Or use a bolder body text.
        style = MaterialTheme.typography.overline,
        fontWeight = FontWeight.Bold // Make it bold to stand out even more
    )
}
