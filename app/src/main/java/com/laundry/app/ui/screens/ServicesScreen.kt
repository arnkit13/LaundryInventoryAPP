package com.laundry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.app.api.LaundryService
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.ui.theme.TealCyanDark
import com.laundry.app.viewmodel.LaundryViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: LaundryViewModel) {
    val services by viewModel.services.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<LaundryService?>(null) }

    var serviceName by remember { mutableStateOf("") }
    var serviceRate by remember { mutableStateOf("") }
    var serviceUnit by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchServices()
    }

    val isAdmin = currentUser?.role == "ROLE_ADMIN"

    val openAddForm = {
        editingService = null
        serviceName = ""
        serviceRate = ""
        serviceUnit = ""
        viewModel.clearFeedback()
        showFormDialog = true
    }

    val openEditForm = { s: LaundryService ->
        editingService = s
        serviceName = s.name
        serviceRate = s.rate.toString()
        serviceUnit = s.unit
        viewModel.clearFeedback()
        showFormDialog = true
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
                    "Services & Pricing Directory",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                
                IconButton(onClick = { viewModel.fetchServices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LaundryBlue)
                }
            }

            // Messages
            successMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(it, color = Color(0xFF2E7D32), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            // Services list
            if (isLoading && services.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (services.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No laundry services created yet.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    services.forEach { s ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        s.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = LaundryBlue
                                    )
                                    Text(
                                        "Rate Unit: ${if (s.unit.isNotEmpty()) s.unit else "global flat"}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "₱${"%.2f".format(s.rate)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = TealCyanDark
                                    )
                                    if (isAdmin) {
                                        IconButton(onClick = { openEditForm(s) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteService(s.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
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
                    Icon(Icons.Default.Add, contentDescription = "Create Service")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Service", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Create/Edit Dialog Form
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = {
                Text(
                    if (editingService == null) "Create Laundry Service" else "Modify Laundry Service",
                    fontWeight = FontWeight.Bold
                )
            },
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
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text("Service Description Name") },
                        placeholder = { Text("e.g. Free Dry promo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = serviceRate,
                        onValueChange = { serviceRate = it },
                        label = { Text("Billing Rate (₱)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = serviceUnit,
                        onValueChange = { serviceUnit = it },
                        label = { Text("Rate Unit Metric") },
                        placeholder = { Text("e.g. wash, dry, pc, kilo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = serviceRate.toDoubleOrNull() ?: -1.0
                        if (editingService == null) {
                            viewModel.createService(serviceName, rate, serviceUnit)
                        } else {
                            viewModel.updateService(editingService!!.id, serviceName, rate, serviceUnit)
                        }
                        
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showFormDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (editingService == null) "Create" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
