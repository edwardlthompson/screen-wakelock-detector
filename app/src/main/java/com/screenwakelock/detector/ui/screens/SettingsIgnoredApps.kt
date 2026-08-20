package com.screenwakelock.detector.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.ignoredAppsSettingsItems(
    ignoredPackages: Set<String>,
    nightIgnoredPackages: Set<String>,
    recentPackages: List<String>,
    packageLabel: (String) -> String,
    onAddAlways: (String) -> Unit,
    onRemoveAlways: (String) -> Unit,
    onAddNight: (String) -> Unit,
    onRemoveNight: (String) -> Unit,
) {
    item {
        Text(
            "Ignored apps",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    item {
        ListItem(
            headlineContent = { Text("Always ignored") },
            supportingContent = {
                Text(
                    if (ignoredPackages.isEmpty()) {
                        "Hidden from History, Home, Insights, and alerts at all hours"
                    } else {
                        "${ignoredPackages.size} app(s) always hidden — remove below to restore"
                    },
                )
            },
        )
    }
    ignoredPackages.forEach { pkg ->
        item(key = "ignored-$pkg") {
            ListItem(
                headlineContent = { Text(packageLabel(pkg)) },
                supportingContent = { Text(pkg) },
                trailingContent = {
                    TextButton(onClick = { onRemoveAlways(pkg) }) { Text("Remove") }
                },
            )
        }
    }
    if (recentPackages.isNotEmpty()) {
        item {
            AddIgnoredDropdown(
                label = "Add always ignored",
                recentPackages = recentPackages,
                packageLabel = packageLabel,
                onAdd = onAddAlways,
            )
        }
    }
    item {
        ListItem(
            headlineContent = { Text("Night-only ignore") },
            supportingContent = {
                Text(
                    if (nightIgnoredPackages.isEmpty()) {
                        "Hidden only during quiet hours (same window as nighttime Insights)"
                    } else {
                        "${nightIgnoredPackages.size} app(s) hidden at night only"
                    },
                )
            },
        )
    }
    nightIgnoredPackages.forEach { pkg ->
        item(key = "night-ignored-$pkg") {
            ListItem(
                headlineContent = { Text(packageLabel(pkg)) },
                supportingContent = { Text(pkg) },
                trailingContent = {
                    TextButton(onClick = { onRemoveNight(pkg) }) { Text("Remove") }
                },
            )
        }
    }
    if (recentPackages.isNotEmpty()) {
        item {
            AddIgnoredDropdown(
                label = "Add night-only ignore",
                recentPackages = recentPackages,
                packageLabel = packageLabel,
                onAdd = onAddNight,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun AddIgnoredDropdown(
    label: String,
    recentPackages: List<String>,
    packageLabel: (String) -> String,
    onAdd: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            recentPackages.forEach { pkg ->
                DropdownMenuItem(
                    text = { Text(packageLabel(pkg)) },
                    onClick = {
                        expanded = false
                        onAdd(pkg)
                    },
                )
            }
        }
    }
}
