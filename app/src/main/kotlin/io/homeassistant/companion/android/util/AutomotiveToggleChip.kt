package io.homeassistant.companion.android.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.EntityPosition
import io.homeassistant.companion.android.common.data.integration.domain
import io.homeassistant.companion.android.common.data.integration.getCoverPosition
import io.homeassistant.companion.android.common.data.integration.getFanSpeed
import io.homeassistant.companion.android.common.data.integration.getLightBrightness
import io.homeassistant.companion.android.common.data.integration.getLightColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A custom composable that mimics a Wear OS ToggleChip, built using Material Design 2
 * for use in Android Automotive OS.
 *
 * It provides a large, easy-to-tap surface with a label and a toggle switch,
 * prioritizing large touch targets and clear text for driver safety.
 *
 * @param label The text to display on the chip.
 * @param checked The current checked state of the switch.
 * @param onCheckedChange A lambda to be invoked when the user clicks the chip.
 * @param modifier The modifier to be applied to the component.
 * @param enabled Controls the enabled state of the component. When false, this component will not
 * be interactive.
 */
@Composable
fun AutomotiveToggleChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Surface provides the "chip" background and shape.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Generous height for easy tapping in a car
            .clip(CircleShape), // Fully rounded corners
        shape = CircleShape,
        // In Material 2, colors are under `MaterialTheme.colors`
        // `surface` is a good default background color.
        color = MaterialTheme.colors.surface,
        elevation = 2.dp // Add a subtle shadow to lift the chip
    ) {
        Row(
            modifier = Modifier
                // The clickable modifier makes the entire Row a single touch target.
                .clickable(
                    enabled = enabled,
                    // Good for accessibility, announces it as a switch.
                    role = Role.Switch,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // The label for the setting
            Text(
                text = label,
                // In Material 2, typography styles are like `body1`, `h6`, etc.
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f) // Text takes up available space
            )

            Spacer(modifier = Modifier.width(16.dp))

            // The actual switch control
            Switch(
                checked = checked,
                // The parent Row handles the click, so we pass null here
                // to avoid double-handling the event. The Switch will still
                // visually react to the `checked` state change.
                onCheckedChange = null,
                enabled = enabled
            )
        }
    }
}

object AutomotiveToggleChipColorsObject {
    /**
     * A function that provides chip colors that mostly follow M3 styling, but for use with M2
     * components that can to, when supported, provide a background for active entities that
     * reflects their state (position and color). M3's ToggleButton does not allow anything more
     * complicated than a single color on a button. Gradient code is based on
     * [androidx.wear.compose.material.ToggleChipDefaults.toggleChipColors].
     *
     * @param entity The entity state on which the background for the active state should be based
     */
    @Composable
    fun entityToggleChipBackgroundColors(entity: Entity<*>, checked: Boolean): AutomotiveToggleChipColors {
        // For a toggleable entity, a custom background should only be used if it has:
        // a. a position (eg. fan speed, light brightness)
        // b. a custom color (eg. light color)
        // If there is a position (a) but no color (b), use the on (surfaceBright) color for the 'active' part.
        // If there is a color (b) but no position (a), use the calculated color for the background.
        // If it doesn't have either or is 'off', it should use the default off (surfaceDim) background.

        val hasPosition = when (entity.domain) {
            "cover" -> entity.state != "closed" && entity.getCoverPosition() != null
            "fan" -> checked && entity.getFanSpeed() != null
            "light" -> checked && entity.getLightBrightness() != null
            else -> false
        }
        val hasColor = entity.getLightColor() != null
        val gradientDirection = LocalLayoutDirection.current

        val contentBackgroundColor = if (hasColor) {
            val entityColor = entity.getLightColor()
            if (entityColor != null) Color(entityColor) else MaterialTheme.colors.onSurface
        } else {
            MaterialTheme.colors.onSurface
        }

        return when {
            (hasPosition || hasColor) -> {
                val checkedStartBackgroundColor = if (hasColor) {
                    contentBackgroundColor.copy(alpha = 0.5f).compositeOver(MaterialTheme.colors.onBackground)
                } else {
                    MaterialTheme.colors.onSurface
                }
                val checkedEndBackgroundColor = if (hasPosition) {
                    MaterialTheme.colors.onBackground // Used as 'off' color
                } else {
                    checkedStartBackgroundColor // On no position = entire background 'on'
                }
                val uncheckedBackgroundColor = MaterialTheme.colors.onBackground

                var checkedBackgroundColors = listOf(
                    checkedStartBackgroundColor,
                    checkedEndBackgroundColor
                )
                var disabledCheckedBackgroundColors = listOf(
                    checkedStartBackgroundColor.copy(alpha = 0.38f),
                    checkedEndBackgroundColor.copy(alpha = 0.38f)
                )
                val uncheckedBackgroundColors = listOf(
                    uncheckedBackgroundColor,
                    uncheckedBackgroundColor
                )
                val disabledUncheckedBackgroundColors = listOf(
                    uncheckedBackgroundColor.copy(alpha = 0.38f),
                    uncheckedBackgroundColor.copy(alpha = 0.38f)
                )
                if (gradientDirection != LayoutDirection.Ltr) {
                    checkedBackgroundColors = checkedBackgroundColors.reversed()
                    disabledCheckedBackgroundColors = disabledCheckedBackgroundColors.reversed()
                    // No need to reverse unchecked
                }

                val checkedBackgroundPaint: Painter
                val disabledCheckedBackgroundPaint: Painter
                val uncheckedBackgroundPaint: Painter
                val disabledUncheckedBackgroundPaint: Painter
                if (hasPosition) {
                    // Insert colors in the middle again to act as a 'hard stop'
                    checkedBackgroundColors = checkedBackgroundColors.toMutableList().apply {
                        addAll(1, checkedBackgroundColors)
                    }
                    disabledCheckedBackgroundColors = disabledCheckedBackgroundColors.toMutableList().apply {
                        addAll(1, disabledCheckedBackgroundColors)
                    }

                    // Use position info to provide stop points
                    // Minimum/maximum stops are not set to 0f/1f to make 1%/100% values visible
                    val position = when (entity.domain) {
                        "cover" -> entity.getCoverPosition()
                        "fan" -> entity.getFanSpeed()
                        "light" -> entity.getLightBrightness()
                        else -> null
                    } ?: EntityPosition(value = 1f, min = 0f, max = 2f) // This should never happen
                    val positionValueRelative = if (gradientDirection == LayoutDirection.Ltr) {
                        ((position.value - position.min) / (position.max - position.min))
                    } else {
                        1 - ((position.value - position.min) / (position.max - position.min))
                    }
                    val checkedColorStops = checkedBackgroundColors.mapIndexed { index, color ->
                        when (index) {
                            0 -> 0.025f to color
                            1, 2 -> positionValueRelative to color
                            else -> 0.975f to color // index: 3
                        }
                    }.toTypedArray()
                    val disabledCheckedColorStops = disabledCheckedBackgroundColors.mapIndexed { index, color ->
                        when (index) {
                            0 -> 0.025f to color
                            1, 2 -> positionValueRelative to color
                            else -> 0.975f to color // index: 3
                        }
                    }.toTypedArray()

                    // Painters that use the color stops
                    // For unchecked with position, we can reuse the checked painter
                    checkedBackgroundPaint = BrushPainter(
                        Brush.horizontalGradient(*checkedColorStops)
                    )
                    disabledCheckedBackgroundPaint = BrushPainter(
                        Brush.horizontalGradient(*disabledCheckedColorStops)
                    )
                    uncheckedBackgroundPaint = BrushPainter(
                        Brush.horizontalGradient(*checkedColorStops)
                    )
                    disabledUncheckedBackgroundPaint = BrushPainter(
                        Brush.horizontalGradient(*disabledCheckedColorStops)
                    )
                } else {
                    // Color should be towards the end to match other enabled ToggleChips
                    // No need to reverse unchecked because it is the same color
                    checkedBackgroundColors = checkedBackgroundColors.reversed()
                    disabledCheckedBackgroundColors = disabledCheckedBackgroundColors.reversed()

                    // Painters that match ToggleChipDefaults
                    checkedBackgroundPaint = BrushPainter(Brush.linearGradient(checkedBackgroundColors))
                    disabledCheckedBackgroundPaint = BrushPainter(Brush.linearGradient(disabledCheckedBackgroundColors))
                    uncheckedBackgroundPaint = BrushPainter(Brush.linearGradient(uncheckedBackgroundColors))
                    disabledUncheckedBackgroundPaint = BrushPainter(Brush.linearGradient(disabledUncheckedBackgroundColors))
                }

                defaultChipColors(
                    checkedContentColor = MaterialTheme.colors.onPrimary,
                    checkedSecondaryContentColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f),
                    uncheckedContentColor = MaterialTheme.colors.onSurface,
                    uncheckedSecondaryContentColor = MaterialTheme.colors.onSecondary
                ).apply {
                    checkedBackgroundPainter = checkedBackgroundPaint
                    disabledCheckedBackgroundPainter = disabledCheckedBackgroundPaint
                    uncheckedBackgroundPainter = uncheckedBackgroundPaint
                    disabledUncheckedBackgroundPainter = disabledUncheckedBackgroundPaint
                }
            }
            else -> defaultChipColors(
                checkedStartBackgroundColor = MaterialTheme.colors.onSurface,
                checkedEndBackgroundColor = MaterialTheme.colors.onSurface,
                checkedContentColor = MaterialTheme.colors.onPrimary,
                checkedSecondaryContentColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f),
                uncheckedStartBackgroundColor = MaterialTheme.colors.onBackground,
                uncheckedEndBackgroundColor = MaterialTheme.colors.onBackground,
                uncheckedContentColor = MaterialTheme.colors.onSurface,
                uncheckedSecondaryContentColor = MaterialTheme.colors.onSecondary
            )
        }
    }

    /**
     * A copy of [androidx.wear.compose.material.ToggleChipDefaults.toggleChipColors] that returns
     * [AutomotiveToggleChipColors] which allows the app to set the Painter for advanced use cases instead
     * of only providing a Color.
     */
    @Composable
    private fun defaultChipColors(
        checkedStartBackgroundColor: Color =
            MaterialTheme.colors.primaryVariant.copy(alpha = 0f)
                .compositeOver(MaterialTheme.colors.surface),
        checkedEndBackgroundColor: Color =
            MaterialTheme.colors.primary.copy(alpha = 0.5f)
                .compositeOver(MaterialTheme.colors.surface),
        checkedContentColor: Color = MaterialTheme.colors.background,
        checkedSecondaryContentColor: Color = MaterialTheme.colors.surface,
        checkedToggleControlColor: Color = MaterialTheme.colors.surface,
        uncheckedStartBackgroundColor: Color = MaterialTheme.colors.primaryVariant,
        uncheckedEndBackgroundColor: Color = uncheckedStartBackgroundColor,
        uncheckedContentColor: Color = MaterialTheme.colors.error,
        uncheckedSecondaryContentColor: Color = uncheckedContentColor,
        uncheckedToggleControlColor: Color = uncheckedContentColor,
        gradientDirection: LayoutDirection = LocalLayoutDirection.current
    ): AutomotiveToggleChipColors {
        val checkedBackgroundColors: List<Color>
        val disabledCheckedBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            checkedBackgroundColors = listOf(
                checkedStartBackgroundColor,
                checkedEndBackgroundColor
            )
            disabledCheckedBackgroundColors = listOf(
                checkedStartBackgroundColor.copy(alpha = 0.38f),
                checkedEndBackgroundColor.copy(alpha = 0.38f)
            )
        } else {
            checkedBackgroundColors = listOf(
                checkedEndBackgroundColor,
                checkedStartBackgroundColor
            )
            disabledCheckedBackgroundColors = listOf(
                checkedEndBackgroundColor.copy(alpha = 0.38f),
                checkedStartBackgroundColor.copy(alpha = 0.38f)
            )
        }
        val uncheckedBackgroundColors: List<Color>
        val disabledUncheckedBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            uncheckedBackgroundColors = listOf(
                uncheckedStartBackgroundColor,
                uncheckedEndBackgroundColor
            )
            disabledUncheckedBackgroundColors = listOf(
                uncheckedStartBackgroundColor.copy(alpha = 0.38f),
                uncheckedEndBackgroundColor.copy(alpha = 0.38f)
            )
        } else {
            uncheckedBackgroundColors = listOf(
                uncheckedEndBackgroundColor,
                uncheckedStartBackgroundColor
            )
            disabledUncheckedBackgroundColors = listOf(
                uncheckedEndBackgroundColor.copy(alpha = 0.38f),
                uncheckedStartBackgroundColor.copy(alpha = 0.38f)
            )
        }

        return AutomotiveToggleChipColors(
            checkedBackgroundPainter = BrushPainter(Brush.linearGradient(checkedBackgroundColors)),
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedToggleControlColor,
            uncheckedBackgroundPainter = BrushPainter(
                Brush.linearGradient(uncheckedBackgroundColors)
            ),
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedToggleControlColor,
            disabledCheckedBackgroundPainter = BrushPainter(
                Brush.linearGradient(disabledCheckedBackgroundColors)
            ),
            disabledCheckedContentColor = checkedContentColor.copy(alpha = 0.38f),
            disabledCheckedSecondaryContentColor = checkedSecondaryContentColor.copy(
                alpha = 0.38f
            ),
            disabledCheckedIconColor = checkedToggleControlColor.copy(
                alpha = 0.38f
            ),
            disabledUncheckedBackgroundPainter = BrushPainter(
                Brush.linearGradient(disabledUncheckedBackgroundColors)
            ),
            disabledUncheckedContentColor = uncheckedContentColor.copy(
                alpha = 0.38f
            ),
            disabledUncheckedSecondaryContentColor = uncheckedSecondaryContentColor.copy(
                alpha = 0.38f
            ),
            disabledUncheckedIconColor = uncheckedToggleControlColor.copy(
                alpha = 0.38f
            )
        )
    }
}

/**
 * A copy of [androidx.wear.compose.material.DefaultToggleChipColors] with a public constructor and mutable
 * properties to allow the app to set the Painter for advanced use cases instead of only providing a Color.
 */
class AutomotiveToggleChipColors(
    var checkedBackgroundPainter: Painter,
    var checkedContentColor: Color,
    var checkedSecondaryContentColor: Color,
    var checkedIconColor: Color,
    var disabledCheckedBackgroundPainter: Painter,
    var disabledCheckedContentColor: Color,
    var disabledCheckedSecondaryContentColor: Color,
    var disabledCheckedIconColor: Color,
    var uncheckedBackgroundPainter: Painter,
    var uncheckedContentColor: Color,
    var uncheckedSecondaryContentColor: Color,
    var uncheckedIconColor: Color,
    var disabledUncheckedBackgroundPainter: Painter,
    var disabledUncheckedContentColor: Color,
    var disabledUncheckedSecondaryContentColor: Color,
    var disabledUncheckedIconColor: Color
) {

    @Composable
    fun background(enabled: Boolean, checked: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedBackgroundPainter else uncheckedBackgroundPainter
            } else {
                if (checked) {
                    disabledCheckedBackgroundPainter
                } else {
                    disabledUncheckedBackgroundPainter
                }
            }
        )
    }

    @Composable
    fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedContentColor else uncheckedContentColor
            } else {
                if (checked) disabledCheckedContentColor else disabledUncheckedContentColor
            }
        )
    }

    @Composable
    fun secondaryContentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedSecondaryContentColor else uncheckedSecondaryContentColor
            } else {
                if (checked) {
                    disabledCheckedSecondaryContentColor
                } else {
                    disabledUncheckedSecondaryContentColor
                }
            }
        )
    }

    @Composable
    fun toggleControlColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedIconColor else uncheckedIconColor
            } else {
                if (checked) disabledCheckedIconColor else disabledUncheckedIconColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as AutomotiveToggleChipColors

        if (checkedBackgroundPainter != other.checkedBackgroundPainter) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (uncheckedBackgroundPainter != other.uncheckedBackgroundPainter) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (disabledCheckedBackgroundPainter != other.disabledCheckedBackgroundPainter) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledCheckedSecondaryContentColor !=
            other.disabledCheckedSecondaryContentColor
        ) {
            return false
        }
        if (disabledUncheckedBackgroundPainter !=
            other.disabledUncheckedBackgroundPainter
        ) {
            return false
        }
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false
        if (disabledUncheckedSecondaryContentColor !=
            other.disabledUncheckedSecondaryContentColor
        ) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = checkedBackgroundPainter.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + uncheckedBackgroundPainter.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedBackgroundPainter.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedBackgroundPainter.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        return result
    }
}
