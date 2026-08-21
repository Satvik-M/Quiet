package com.satvikm.quiet.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = label, color = onBackground.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.padding(top = 4.dp)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Text(
                    text = option,
                    color = if (selected) onBackground else onBackground.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { onSelect(index) }
                        .padding(end = 24.dp),
                )
            }
        }
    }
}
