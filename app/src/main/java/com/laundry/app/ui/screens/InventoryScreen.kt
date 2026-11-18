package com.laundry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.laundry.app.api.SoapProduct
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: LaundryViewModel) {
    val products by viewModel.products.collectAsState()
    val historyLogs by viewModel.inventoryHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Stock Ledger, 1 = Stock History Logs

    var showAddDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var targetProductForAdjustment by remember { mutableStateOf<SoapProduct?>(null) }

    // Add Form fields
    var newProdName by remember { mutableStateOf("") }
    var newProdQty by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("kg") }

    // Adjust Form fields
    var adjustDirection by remember { mutableStateOf("ADD") } // "ADD" or "USE"
    var adjustQty by remember { mutableStateOf("") }
    var adjustNotes by remember { mutableStateOf("") }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            viewModel.fetchInventory()
        } else {
            viewModel.fetchInventoryHistory()
        }
    }

    val openAddForm = {
        newProdName = ""
        newProdQty = ""
        newProdUnit = "kg"
        viewModel.clearFeedback()
        showAddDialog = true
    }

    val openAdjustForm = { p: SoapProduct ->
        targetProductForAdjustment = p
        adjustDirection = "ADD"
        adjustQty = ""
        adjustNotes = ""
        viewModel.clearFeedback()
        showAdjustDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = TealCyan
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Inventory Manager",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                
                IconButton(onClick = {
                    if (selectedTab == 0) viewModel.fetchInventory() else viewModel.fetchInventoryHistory()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LaundryBlue)
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = LaundryBlue,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = LaundryBlue,
                    unselectedContentColor = Color.Gray,
                    text = { Text("Stock Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = LaundryBlue,
                    unselectedContentColor = Color.Gray,
                    text = { Text("Stock Audit Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            // Success or Error banner
            successMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(it, color = Color(0xFF2E7D32), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            // Tab Content
            if (isLoading && products.isEmpty() && historyLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (products.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No inventory items registered.", color = Color.Gray)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                products.forEach { p ->
                                    val isLow = p.quantity < (p.minStock ?: 5.0)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            p.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp,
                                                            color = Color.Black
                                                        )
                                                        if (isLow) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Icon(
                                                                Icons.Default.Warning,
                                                                contentDescription = "Low Stock",
                                                                tint = Color(0xFFE65100),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        "Min Alert Level: ${p.minStock ?: 5.0} ${p.unit} • Initial Seed: ${p.initialStock ?: 0.0} ${p.unit}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }

                                                Text(
                                                    "${p.quantity} ${p.unit}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 18.sp,
                                                    color = if (isLow) Color(0xFFC81E1E) else Color(0xFF2E7D32)
                                                )
                                            }

                                            Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { openAdjustForm(p) },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = LaundryBlue)
                                                ) {
                                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Adjust Stock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                
                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = { viewModel.deleteSoapProduct(p.id) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (historyLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No inventory audit logs recorded.", color = Color.Gray)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                historyLogs.forEach { log ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    log.soapProduct.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                
                                                val isAdd = log.transactionType == "ADD_STOCK"
                                                val sign = if (isAdd) "+" else ""
                                                val color = if (isAdd) Color(0xFF2E7D32) else Color(0xFFC81E1E)
                                                Text(
                                                    "$sign${log.quantityChanged} ${log.soapProduct.unit}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = color
                                                )
                                            }

                                            Text(
                                                "Action: ${log.transactionType} • Performer: ${log.performedBy.fullName}",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Audit Levels: ${log.previousQuantity} -> ${log.newQuantity} ${log.soapProduct.unit}",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                                Text(
                                                    log.createdAt.split("T")[0],
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            if (!log.notes.isNullOrEmpty()) {
                                                Text(
                                                    "Note: ${log.notes}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 4.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp)).padding(6.dp).fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { openAddForm() },
                containerColor = LaundryBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Resource")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Resource", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Add Product Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register Inventory Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    }

                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Resource Item Name") },
                        placeholder = { Text("e.g. Liquid Softener") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = newProdQty,
                        onValueChange = { newProdQty = it },
                        label = { Text("Initial Stock Count") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    var unitExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newProdUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Metric Unit") },
                            trailingIcon = {
                                IconButton(onClick = { unitExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { unitExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }, modifier = Modifier.background(Color.White)) {
                            listOf("kg", "L", "pcs", "sachet", "LOAD").forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u, color = Color.Black) },
                                    onClick = {
                                        newProdUnit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = newProdQty.toDoubleOrNull() ?: -1.0
                        viewModel.createSoapProduct(newProdName, qty, newProdUnit)
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Adjust Stock Dialog
    if (showAdjustDialog && targetProductForAdjustment != null) {
        val p = targetProductForAdjustment!!
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Adjust Stock: ${p.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    }

                    // ADD or USE toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { adjustDirection = "ADD" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adjustDirection == "ADD") Color(0xFF2E7D32) else Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Top Up (+)", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { adjustDirection = "USE" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adjustDirection == "USE") Color(0xFFC81E1E) else Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Consume (-)", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = adjustQty,
                        onValueChange = { adjustQty = it },
                        label = { Text("Adjustment Quantity Amount") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text(p.unit) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = adjustNotes,
                        onValueChange = { adjustNotes = it },
                        label = { Text("Adjustment Notes / Reason") },
                        placeholder = { Text("Purchase restock, spills, manual correction...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = adjustQty.toDoubleOrNull() ?: 0.0
                        val changed = if (adjustDirection == "ADD") qty else -qty
                        viewModel.adjustStock(p.id, changed, adjustNotes)
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showAdjustDialog = false
                            }
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
