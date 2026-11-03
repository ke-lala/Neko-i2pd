package org.purplei2p.i2pd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact title treatment shared by the primary screens. The index is decorative only. */
@Composable
internal fun ScreenHeading(title: Int, index: String, modifier: Modifier = Modifier, horizontalInset: Dp = 16.dp) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = horizontalInset, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                index,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp).clearAndSetSemantics { },
            )
            Text(
                text = stringResource(title),
                fontSize = 29.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 5.dp),
            thickness = 0.75.dp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
