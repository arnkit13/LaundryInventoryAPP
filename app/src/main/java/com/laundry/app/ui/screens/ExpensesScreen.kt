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
import com.laundry.app.api.Expense
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val EXPENSE_CATEGORIES = listOf(
    "Payroll", "Detergent", "Maintenance", "Fabric conditioner",
    "xonrox", "tape", "cellophane", "GASOL", "Utilities",
    "SALARY", "ELECTRIC BILL", "WATER BILL"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: LaundryViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    // Form Field States
    var category by remember { mutableStateOf("Detergent") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedBranchId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchExpenses()
        viewModel.fetchBranches()
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    val totalExpenseSum = expenses.sumOf { it.amount }
    val averageCost = if (expenses.isNotEmpty()) totalExpenseSum / expenses.size else 0.0

    val openAddForm = {
        editingExpense = null
        category = "Detergent"
        amount = ""
        date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        description = ""
        selectedBranchId = null
        viewModel.clearFeedback()
        showFormDialog = true
    }

    val openEditForm = { exp: Expense ->
        editingExpense = exp
        category = exp.category
        amount = exp.amount.toString()
        date = exp.date
        description = exp.description ?: ""
        selectedBranchId = exp.branch?.id
        viewModel.clearFeedback()
        showFormDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                    "Expenses Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                IconButton(onClick = { viewModel.fetchExpenses() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LaundryBlue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Total Operational",
                        value = currencyFormatter.format(totalExpenseSum),
                        bgColor = Color(0xFFFDE8E8),
                        borderColor = Color(0xFFF8B4B4),
                        textColor = Color(0xFFC81E1E),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Entries",
                        value = "${expenses.size} Logs",
                        bgColor = Color(0xFFFFF9C4),
                        borderColor = Color(0xFFFFF176),
                        textColor = Color(0xFFF57F17),
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricCard(
                    title = "Average Cost",
                    value = currencyFormatter.format(averageCost),
                    bgColor = Color(0xFFE0F2F1),
                    borderColor = Color(0xFF80CBC4),
                    textColor = Color(0xFF004D40),
                    modifier = Modifier.fillMaxWidth()
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

            // List of Expenses
            if (isLoading && expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses logged in system.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenses.forEach { exp ->
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
                                        exp.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = LaundryBlue
                                    )
                                    Text(
                                        "${exp.date} • ${exp.branch?.name ?: "All Branches (Global)"}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (!exp.description.isNullOrEmpty()) {
                                        Text(
                                            exp.description,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        currencyFormatter.format(exp.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFC81E1E),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { openEditForm(exp) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteExpense(exp.id) },
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

        ExtendedFloatingActionButton(
            onClick = { openAddForm() },
            containerColor = LaundryBlue,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Record Expense", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    // Add/Edit Form Dialog
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = {
                Text(
                    if (editingExpense == null) "Record Expense Entry" else "Modify Expense Entry",
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

                    // Category Dropdown Selection
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Category") },
                            trailingIcon = {
                                IconButton(onClick = { categoryExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.background(Color.White)) {
                            EXPENSE_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = Color.Black) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount paid
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount Paid (₱)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Date
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date of Transaction (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 2026-07-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Branch Assignment Dropdown
                    var branchExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = branches.find { it.id == selectedBranchId }?.name ?: "All Branches (Global)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Branch Assignment") },
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

                    // Description text area
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Description") },
                        placeholder = { Text("Utility billing description, purchase detail...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = amount.toDoubleOrNull() ?: -1.0
                        if (editingExpense == null) {
                            viewModel.createExpense(category, amountVal, date, description, selectedBranchId)
                        } else {
                            viewModel.updateExpense(editingExpense!!.id, category, amountVal, date, description, selectedBranchId)
                        }
                        // Check if success triggers closing the dialog
                        viewModel.viewModelScope.launch {
                            kotlinx.coroutines.delay(200)
                            if (viewModel.errorMessage.value == null) {
                                showFormDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (editingExpense == null) "Record" else "Update")
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
