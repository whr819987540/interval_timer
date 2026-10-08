package app.intervaltimer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing

/**
 * Round icon button with a visible text label underneath. The label doubles as the accessible
 * name, so screen readers announce it once.
 */
@Composable
fun LabeledIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    size: Dp = if (emphasized) Sizes.heroAction else Sizes.secondaryAction,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val iconContent: @Composable () -> Unit = {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(if (emphasized) Sizes.iconLg else Sizes.iconMd),
            )
        }
        if (emphasized) {
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(size),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                content = iconContent,
            )
        } else {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = Modifier.size(size),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                content = iconContent,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
