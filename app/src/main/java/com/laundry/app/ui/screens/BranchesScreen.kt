package com.laundry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay

data class EditableMachine(val tempId: Int, val name: String, val available: Boolean)

fun parseMachinesConfig(config: String?): List<EditableMachine> {
    if (config.isNullOrEmpty()) {
        return listOf(
            EditableMachine(1, "Machine 1", true),
            EditableMachine(2, "Machine 2", true),
            EditableMachine(3, "Machine 3", true),
            EditableMachine(4, "Machine 4", true)
        )
    }
    return config.split(",").mapIndexedNotNull { idx, item ->
        val parts = item.split(":")
        if (parts.size >= 2) {
            val name = parts[0].trim()
            val available = parts[1].trim().lowercase() == "true"
            EditableMachine(idx + 1, name, available)
        } else if (parts.isNotEmpty()) {
            val name = parts[0].trim()
            EditableMachine(idx + 1, name, true)
        } else {
            null
        }
    }
}

fun serializeMachinesConfig(machines: List<EditableMachine>): String {
    return machines.joinToString(",") { "${it.name}:${it.available}" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesScreen(viewModel: LaundryViewModel) {
    val branches by viewModel.branches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedBranch by remember { mutableStateOf<com.laundry.app.api.Branch?>(null) }

    var branchName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var editBranchName by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var machinesList by remember { mutableStateOf(listOf<EditableMachine>()) }

    LaunchedEffect(Unit) {
        viewModel.fetchBranches()
    }

    val openAddForm = {
        branchName = ""
        location = ""
        machinesList = listOf(
            EditableMachine(1, "Machine 1", true),
            EditableMachine(2, "Machine 2", true),
            EditableMachine(3, "Machine 3", true),
            EditableMachine(4, "Machine 4", true)
        )
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
                    "Branch Directory",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                
                IconButton(onClick = { viewModel.fetchBranches() }) {
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

            // List of branches
            if (isLoading && branches.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (branches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No laundry branches established yet.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    branches.forEach { b ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBranch = b
                                    editBranchName = b.name
                                    editLocation = b.location
                                    machinesList = parseMachinesConfig(b.machinesConfig)
                                    viewModel.clearFeedback()
                                    showEditDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        tint = TealCyan,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            b.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            b.location,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBranch(b.id) },
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

        ExtendedFloatingActionButton(
            onClick = { openAddForm() },
            containerColor = LaundryBlue,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Create Branch", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    // Modal Create Branch Dialog Form
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = { Text("Establish New Branch", fontWeight = FontWeight.Bold) },
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
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("Branch Name") },
                        placeholder = { Text("e.g. North Avenue Express") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Branch Physical Address") },
                        placeholder = { Text("e.g. Brgy. Bagong Pag-asa, Quezon City") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Configure Machines", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(
                            onClick = {
                                machinesList = machinesList + EditableMachine(
                                    tempId = (machinesList.maxOfOrNull { it.tempId } ?: 0) + 1,
                                    name = "Machine ${machinesList.size + 1}",
                                    available = true
                                )
                            }
                        ) {
                            Text("+ Add Machine", fontSize = 12.sp, color = TealCyan)
                        }
                    }

                    if (machinesList.isEmpty()) {
                        Text("No machines configured. Add at least one.", color = Color.Gray, fontSize = 12.sp)
                    }

                    machinesList.forEachIndexed { idx, machine ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = machine.name,
                                onValueChange = { newName ->
                                    machinesList = machinesList.mapIndexed { i, m ->
                                        if (i == idx) m.copy(name = newName) else m
                                    }
                                },
                                label = { Text("Machine Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    machinesList = machinesList.mapIndexed { i, m ->
                                        if (i == idx) m.copy(available = !m.available) else m
                                    }
                                }
                            ) {
                                Checkbox(
                                    checked = machine.available,
                                    onCheckedChange = { isChecked ->
                                        machinesList = machinesList.mapIndexed { i, m ->
                                            if (i == idx) m.copy(available = isChecked) else m
                                        }
                                    }
                                )
                                Text("Active", fontSize = 11.sp)
                            }
                            
                            IconButton(
                                onClick = {
                                    machinesList = machinesList.filterIndexed { i, _ -> i != idx }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createBranch(branchName, location, serializeMachinesConfig(machinesList))
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showFormDialog = false
                            }
                        }
                    }
                ) {
                    Text("Establish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog && selectedBranch != null) {
        val b = selectedBranch!!
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Configure Branch Details", fontWeight = FontWeight.Bold) },
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
                        value = editBranchName,
                        onValueChange = { editBranchName = it },
                        label = { Text("Branch Name") },
                        placeholder = { Text("e.g. North Avenue Express") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Branch Physical Address") },
                        placeholder = { Text("e.g. Brgy. Bagong Pag-asa, Quezon City") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Configure Machines", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(
                            onClick = {
                                machinesList = machinesList + EditableMachine(
                                    tempId = (machinesList.maxOfOrNull { it.tempId } ?: 0) + 1,
                                    name = "Machine ${machinesList.size + 1}",
                                    available = true
                                )
                            }
                        ) {
                            Text("+ Add Machine", fontSize = 12.sp, color = TealCyan)
                        }
                    }

                    if (machinesList.isEmpty()) {
                        Text("No machines configured. Add at least one.", color = Color.Gray, fontSize = 12.sp)
                    }

                    machinesList.forEachIndexed { idx, machine ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = machine.name,
                                onValueChange = { newName ->
                                    machinesList = machinesList.mapIndexed { i, m ->
                                        if (i == idx) m.copy(name = newName) else m
                                    }
                                },
                                label = { Text("Machine Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    machinesList = machinesList.mapIndexed { i, m ->
                                        if (i == idx) m.copy(available = !m.available) else m
                                    }
                                }
                            ) {
                                Checkbox(
                                    checked = machine.available,
                                    onCheckedChange = { isChecked ->
                                        machinesList = machinesList.mapIndexed { i, m ->
                                            if (i == idx) m.copy(available = isChecked) else m
                                        }
                                    }
                                )
                                Text("Active", fontSize = 11.sp)
                            }
                            
                            IconButton(
                                onClick = {
                                    machinesList = machinesList.filterIndexed { i, _ -> i != idx }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBranch(b.id, editBranchName, editLocation, serializeMachinesConfig(machinesList))
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showEditDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
