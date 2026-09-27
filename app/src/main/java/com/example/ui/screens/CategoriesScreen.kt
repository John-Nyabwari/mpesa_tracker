package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CategoryEntity
import com.example.ui.theme.*

private val SWATCH_COLORS = listOf(
    "#244C4E", "#58C411", "#337D63", "#89BA4F",
    "#804B35", "#C62828", "#1976D2", "#F57C00",
    "#7B1FA2", "#00838F", "#5D4037", "#4C5267"
)

private fun parseSafeColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        DarkGreen
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCategory: (String, String) -> Unit,
    onUpdateCategory: (oldName: String, newName: String, colorHex: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories (${categories.size})", fontWeight = FontWeight.Bold, color = DarkGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("categories_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = DarkGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.name }) { cat ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(parseSafeColor(cat.colorHex), shape = CircleShape)
                            )
                            Column {
                                Text(cat.name, fontWeight = FontWeight.Bold, color = DarkGreen, fontSize = 14.sp)
                                Text(
                                    if (cat.isDefault) "System Default • ${cat.colorHex}" else "Custom • ${cat.colorHex}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Row {
                            IconButton(onClick = { editingCategory = cat }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = DarkGreen)
                            }
                            if (!cat.isDefault) {
                                IconButton(onClick = { onDeleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = ExpenseRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryFormDialog(
            title = "Create Category",
            initialName = "",
            initialHex = SWATCH_COLORS.first(),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, hex ->
                onAddCategory(name, hex)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { cat ->
        CategoryFormDialog(
            title = "Edit Category",
            initialName = cat.name,
            initialHex = cat.colorHex,
            onDismiss = { editingCategory = null },
            onConfirm = { newName, newHex ->
                onUpdateCategory(cat.name, newName, newHex)
                editingCategory = null
            }
        )
    }
}

@Composable
private fun CategoryFormDialog(
    title: String,
    initialName: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var colorHex by remember(initialHex) { mutableStateOf(initialHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = DarkGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                Text("Select Swatch:", fontSize = 12.sp, color = TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SWATCH_COLORS) { swatch ->
                        val isSelected = swatch.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseSafeColor(swatch))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) BrandLime else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = swatch }
                        )
                    }
                }

                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Color Hex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), colorHex.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
