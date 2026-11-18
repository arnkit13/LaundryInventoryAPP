package com.laundry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.app.api.User
import com.laundry.app.api.UserRequest
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.LaundryBlueLight
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: LaundryViewModel) {
    val users by viewModel.users.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }

    // Form states
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("ROLE_USER") }
    var selectedBranchId by remember { mutableStateOf<Long?>(null) }
    var active by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.fetchUsers()
        viewModel.fetchBranches()
    }

    val openAddForm = {
        editingUser = null
        username = ""
        password = ""
        fullName = ""
        role = "ROLE_USER"
        selectedBranchId = null
        active = true
        viewModel.clearFeedback()
        showFormDialog = true
    }

    val openEditForm = { u: User ->
        editingUser = u
        username = u.username
        password = "" // empty means don't change password
        fullName = u.fullName
        role = u.role
        selectedBranchId = u.branch?.id
        active = u.active
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
                    "Employee Management",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                
                IconButton(onClick = { viewModel.fetchUsers() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LaundryBlue)
                }
            }

            // Feedback messages
            successMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(it, color = Color(0xFF2E7D32), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            // User list items
            if (isLoading && users.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No operator accounts found.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    users.forEach { u ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (u.active) LaundryBlue else Color.LightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            u.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (u.active) Color.Black else Color.Gray
                                        )
                                        Text(
                                            "Username: ${u.username} • Role: ${if (u.role == "ROLE_ADMIN") "Admin" else "Employee"}",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            "Assigned Branch: ${u.branch?.name ?: "All Branches (Global)"}",
                                            fontSize = 11.sp,
                                            color = LaundryBlueLight,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Status Switch
                                    Switch(
                                        checked = u.active,
                                        onCheckedChange = { viewModel.toggleUserActive(u.id) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TealCyan)
                                    )

                                    IconButton(
                                        onClick = { openEditForm(u) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteUser(u.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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
                Icon(Icons.Default.Add, contentDescription = "Add User")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add User", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal Add/Edit dialog form
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = {
                Text(
                    if (editingUser == null) "Create Operator Account" else "Modify Operator Account",
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
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. employee1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (editingUser == null) "Password" else "New Password (Optional)") },
                        placeholder = { Text(if (editingUser == null) "Password must not be blank" else "Leave blank to keep current") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Juan Dela Cruz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Role selector
                    var roleExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (role == "ROLE_ADMIN") "Admin" else "Employee",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = {
                                IconButton(onClick = { roleExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { roleExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }, modifier = Modifier.background(Color.White)) {
                            DropdownMenuItem(
                                text = { Text("Employee", color = Color.Black) },
                                onClick = {
                                    role = "ROLE_USER"
                                    roleExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Admin", color = Color.Black) },
                                onClick = {
                                    role = "ROLE_ADMIN"
                                    roleExpanded = false
                                }
                            )
                        }
                    }

                    // Branch assignment
                    var branchExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = branches.find { it.id == selectedBranchId }?.name ?: "All Branches (Global)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Branch") },
                            trailingIcon = {
                                IconButton(onClick = { branchExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { branchExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = branchExpanded, onDismissRequest = { branchExpanded = false }, modifier = Modifier.background(Color.White)) {
                            DropdownMenuItem(
                                text = { Text("All Branches (Global / Shared)", color = Color.Black) },
                                onClick = {
                                    selectedBranchId = null
                                    branchExpanded = false
                                }
                            )
                            branches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name, color = Color.Black) },
                                    onClick = {
                                        selectedBranchId = b.id
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val req = UserRequest(
                            username = username.trim(),
                            password = password.trim().ifEmpty { null },
                            fullName = fullName.trim(),
                            role = role,
                            active = active,
                            branchId = selectedBranchId
                        )
                        if (editingUser == null) {
                            viewModel.createUser(req)
                        } else {
                            viewModel.updateUser(editingUser!!.id, req)
                        }
                        
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showFormDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (editingUser == null) "Create" else "Save")
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
