package com.laundry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
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
import com.laundry.app.ui.theme.TealCyanDark
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: LaundryViewModel) {
    var reportType by remember { mutableStateOf("daily") } // "daily", "weekly", "monthly"
    var selectedBranchId by remember { mutableStateOf<Long?>(null) }

    val reports by viewModel.reports.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(reportType, selectedBranchId) {
        viewModel.fetchReports(reportType, selectedBranchId)
        viewModel.fetchBranches()
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
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
                    "Operational Reports",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                IconButton(onClick = { viewModel.fetchReports(reportType, selectedBranchId) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LaundryBlue)
                }
            }

            // Filters Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Report Type Spinner
                        var typeExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = reportType.replaceFirstChar { it.titlecase(Locale.US) },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Report Type") },
                                trailingIcon = {
                                    IconButton(onClick = { typeExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true },
                                shape = RoundedCornerShape(8.dp)
                            )
                            DropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                listOf("daily", "weekly", "monthly").forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.replaceFirstChar { it.titlecase(Locale.US) }, color = Color.Black) },
                                        onClick = {
                                            reportType = t
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Branch filter spinner
                        var branchExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = branches.find { it.id == selectedBranchId }?.name ?: "All Branches",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Branch Filter") },
                                trailingIcon = {
                                    IconButton(onClick = { branchExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { branchExpanded = true },
                                shape = RoundedCornerShape(8.dp)
                            )
                            DropdownMenu(
                                expanded = branchExpanded,
                                onDismissRequest = { branchExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Branches", color = Color.Black) },
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
                    }
                }
            }

            // Error display banner
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            // Report items results
            if (isLoading && reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records found for this period filter.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reports.forEach { item ->
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
                                        item.period,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = LaundryBlue
                                    )
                                    Text(
                                        currencyFormatter.format(item.totalRevenue),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = TealCyanDark
                                    )
                                }

                                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Row 1: Core Metrics
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Washes: ${item.transactionCount}", fontSize = 12.sp, color = Color.DarkGray)
                                            Text("Load Weight: ${"%.2f".format(item.totalKgWashed)} kg", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                                        }
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text("Soap Used: ${"%.2f".format(item.totalSoapUsed)} units", fontSize = 12.sp, color = Color.DarkGray)
                                            Text("Customers: ${item.customerCount}", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                    
                                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    // Row 2: Performance Averages
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val avg = if (item.transactionCount > 0) item.totalRevenue / item.transactionCount else 0.0
                                        val revPerKg = if (item.totalKgWashed > 0) item.totalRevenue / item.totalKgWashed else 0.0
                                        Text("Avg Rev/Tx: ₱${"%.1f".format(avg)}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Rev/Kg: ₱${"%.1f".format(revPerKg)}", fontSize = 11.sp, color = Color.Gray)
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
