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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.app.api.ServiceItemRequest
import com.laundry.app.api.TransactionRequest
import com.laundry.app.api.LaundryTransaction
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.LaundryBlueLight
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun calculateDurationInShop(createdAtStr: String?, pickedUpAtStr: String?): String {
    if (createdAtStr.isNullOrEmpty()) return "At shop"
    
    val formatters = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    )
    
    var createdTime: Long = 0
    for (formatter in formatters) {
        try {
            createdTime = formatter.parse(createdAtStr)?.time ?: 0
            if (createdTime > 0) break
        } catch (e: Exception) {}
    }
    if (createdTime == 0L) return "At shop"
    
    val endTime = if (!pickedUpAtStr.isNullOrEmpty()) {
        var claimTime: Long = 0
        for (formatter in formatters) {
            try {
                claimTime = formatter.parse(pickedUpAtStr)?.time ?: 0
                if (claimTime > 0) break
            } catch (e: Exception) {}
        }
        if (claimTime > 0L) claimTime else System.currentTimeMillis()
    } else {
        System.currentTimeMillis()
    }
    
    val diffMs = endTime - createdTime
    if (diffMs < 0) return "Just added"
    val diffMins = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)
    val diffDays = diffMs / (1000 * 60 * 60 * 24)
    
    return when {
        diffDays > 0 -> "${diffDays}d ${diffHours % 24}h"
        diffHours > 0 -> "${diffHours}h ${diffMins % 60}m"
        else -> "${diffMins}m"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(viewModel: LaundryViewModel, onNavigateToTransactions: () -> Unit) {
    val stats by viewModel.dashboardStats.collectAsState()
    val products by viewModel.products.collectAsState()
    val servicesList by viewModel.services.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Form inputs state
    val currentUser by viewModel.currentUser.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val branch = remember(branches, currentUser) {
        branches.find { it.id == currentUser?.branchId }
    }
    val machines = remember(branch?.machinesConfig) {
        parseMachinesConfig(branch?.machinesConfig)
    }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var category by remember { mutableStateOf("laundry") }
    var customerName by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var machineNumber by remember(machines) { mutableStateOf(machines.firstOrNull { it.available }?.name ?: "") }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var soapUsedQty by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNumber by remember { mutableStateOf("") }
    
    // selectedServices: serviceId -> quantity
    val selectedServices = remember { mutableStateMapOf<Long, Int>() }
    var showServicesDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var generatedReceipt by remember { mutableStateOf<LaundryTransaction?>(null) }

    // Claim Confirmation Dialog State
    var openClaimModal by remember { mutableStateOf(false) }
    var targetClaimTx by remember { mutableStateOf<LaundryTransaction?>(null) }
    var isClaiming by remember { mutableStateOf(false) }

    // Load initial screen data
    LaunchedEffect(Unit) {
        viewModel.fetchDashboardStats(null, null)
        viewModel.fetchInventory()
        viewModel.fetchServices()
        viewModel.fetchTransactions()
        viewModel.fetchBranches()
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    val calculateTotalBillingPrice = {
        selectedServices.entries.sumOf { (sId, qty) ->
            val s = servicesList.find { it.id == sId }
            (s?.rate ?: 0.0) * qty
        }
    }

    val selectedProduct = products.find { it.id == selectedProductId }
    val remainingSoapForecast = if (selectedProduct != null && soapUsedQty.isNotEmpty()) {
        val used = soapUsedQty.toDoubleOrNull() ?: 0.0
        selectedProduct.quantity - used
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

            // Branding Card Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LaundryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tacky's Laundry",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Terminal Portal & Employee Workspace",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Success or Error banner
            successMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(it, color = Color(0xFF2E7D32), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            // Two panels layout
            Text(
                "Record New Laundry Wash",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LaundryBlueDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Date field
                    val context = LocalContext.current
                    val calendar = Calendar.getInstance()
                    val datePickerDialog = remember {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedMonth = String.format(Locale.US, "%02d", month + 1)
                                val formattedDay = String.format(Locale.US, "%02d", dayOfMonth)
                                date = "$year-$formattedMonth-$formattedDay"
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }

                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction Date *") },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selector
                    Text(
                        "Transaction Category *",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("laundry", "Gym", "pickleball").forEach { cat ->
                            val isSelected = category == cat
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LaundryBlue else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                                    .clickable { 
                                        category = cat
                                        if (cat != "laundry") {
                                            selectedProductId = null
                                            soapUsedQty = ""
                                            weightKg = ""
                                        }
                                    }
                            ) {
                                Text(
                                    text = cat.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Customer Name & Weight fields
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer's Name (Optional)") },
                        placeholder = { Text("Enter customer name...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (category == "laundry") {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = weightKg,
                            onValueChange = { weightKg = it },
                            label = { Text("Weight (kg)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text("kg") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Machine selector
                        Text(
                            "Washing Machine Unit *",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val machineChunks = machines.chunked(4)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            machineChunks.forEach { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    chunk.forEach { machine ->
                                        val isSelected = machineNumber == machine.name
                                        val isAvailable = machine.available
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isSelected -> Color(0xFFE8F4FC)
                                                    !isAvailable -> Color(0xFFF1F5F9)
                                                    else -> Color.White
                                                }
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = when {
                                                        isSelected -> LaundryBlue
                                                        !isAvailable -> Color.LightGray.copy(alpha = 0.5f)
                                                        else -> Color.LightGray
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable(enabled = isAvailable) { machineNumber = machine.name }
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = machine.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isSelected -> LaundryBlue
                                                        !isAvailable -> Color.Gray
                                                        else -> Color.Black
                                                    },
                                                    textAlign = TextAlign.Center
                                                )
                                                if (!isAvailable) {
                                                    Text(
                                                        text = "Out of Order",
                                                        fontSize = 8.sp,
                                                        color = Color.Red,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    val paddingNeeded = 4 - chunk.size
                                    if (paddingNeeded > 0) {
                                        Spacer(modifier = Modifier.weight(paddingNeeded.toFloat()))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Services dialog selector
                    Text(
                        "Select Services & Quantities *",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Button(
                        onClick = { showServicesDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = LaundryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, LaundryBlue, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Services (${selectedServices.size} Selected)", fontWeight = FontWeight.Bold)
                    }

                    if (selectedServices.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedServices.forEach { (id, qty) ->
                                val s = servicesList.find { it.id == id }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${s?.name ?: "Service"} (x$qty)",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₱${"%.2f".format((s?.rate ?: 0.0) * qty)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color.Red,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { selectedServices.remove(id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Resource Product selectors
                    if (category == "laundry") {
                        // Resource Product selectors
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, LaundryBlueLight, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Inventory & Resource Consumables",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaundryBlueDark,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Soap Dropdown
                                var soapExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedProduct?.name ?: "Select Resource Item...",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Inventory Item Used") },
                                        trailingIcon = {
                                            IconButton(onClick = { soapExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { soapExpanded = true },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    DropdownMenu(expanded = soapExpanded, onDismissRequest = { soapExpanded = false }, modifier = Modifier.background(Color.White)) {
                                        products.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("${p.name} (${p.quantity} ${p.unit} left)", color = Color.Black) },
                                                onClick = {
                                                    selectedProductId = p.id
                                                    soapExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = soapUsedQty,
                                    onValueChange = { soapUsedQty = it },
                                    label = { Text("Quantity Utilized") },
                                    placeholder = { Text("0.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    suffix = { Text(selectedProduct?.unit ?: "unit") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // Forecast warning badge
                                remainingSoapForecast?.let { f ->
                                    val isNegative = f < 0
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isNegative) Color(0xFFFDE8E8) else Color(0xFFE8F5E9)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .border(
                                                1.dp,
                                                if (isNegative) Color(0xFFF8B4B4) else Color(0xFFC5E1A5),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Remaining Stock Forecast:",
                                                fontSize = 11.sp,
                                                color = if (isNegative) Color(0xFFC81E1E) else Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${"%.2f".format(f)} ${selectedProduct?.unit ?: ""}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isNegative) Color(0xFFC81E1E) else Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode of Payment
                    Text(
                        "Mode of Payment *",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Cash", "Gcash").forEach { mop ->
                            val isSelected = paymentMethod == mop
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LaundryBlue else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                                    .clickable { paymentMethod = mop }
                            ) {
                                Text(
                                    text = mop,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GCash fields
                    if (paymentMethod.lowercase() == "gcash") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Scan & Pay GCash QR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaundryBlueDark
                                )
                                Text(
                                    "GCash Account: 000",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )
                                
                                // Placeholder QR box
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(150.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(60.dp))
                                            Text("Scan QR Code", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = referenceNumber,
                                    onValueChange = { referenceNumber = it },
                                    label = { Text("Last 4 digits of GCash Ref") },
                                    placeholder = { Text("e.g. 1234") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Total billing overview card
                    if (selectedServices.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Billing Price:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF33691E)
                                )
                                Text(
                                    "₱${"%.2f".format(calculateTotalBillingPrice())}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF33691E)
                                )
                            }
                        }
                    }

                    // Submission
                    val isSubmittingDisabled = isLoading || (remainingSoapForecast != null && remainingSoapForecast < 0.0)
                    Button(
                        onClick = {
                            val request = TransactionRequest(
                                date = date,
                                customerName = customerName,
                                weightKg = if (category == "laundry") (weightKg.toDoubleOrNull() ?: 0.0) else null,
                                soapProductId = if (category == "laundry") selectedProductId else null,
                                soapUsedQty = if (category == "laundry") (soapUsedQty.toDoubleOrNull() ?: 0.0) else null,
                                machineNumber = if (category == "laundry") machineNumber else null,
                                paymentMethod = paymentMethod,
                                referenceNumber = referenceNumber,
                                services = selectedServices.map { (id, qty) ->
                                    ServiceItemRequest(id, qty)
                                },
                                category = category
                            )
                            viewModel.createTransaction(request) { success ->
                                if (success) {
                                    // Reset Form
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    category = "laundry"
                                    customerName = ""
                                    weightKg = ""
                                    soapUsedQty = ""
                                    selectedProductId = null
                                    machineNumber = machines.firstOrNull { it.available }?.name ?: ""
                                    paymentMethod = "Cash"
                                    referenceNumber = ""
                                    selectedServices.clear()
                                    
                                    // Trigger receipt popup
                                    generatedReceipt = viewModel.transactions.value.firstOrNull()
                                    showReceiptDialog = true
                                    viewModel.fetchDashboardStats(null, null)
                                }
                            }
                        },
                        enabled = !isSubmittingDisabled,
                        colors = ButtonDefaults.buttonColors(containerColor = LaundryBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Record Transaction", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Summary metrics cards
            Text(
                "My Record Totals Today",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LaundryBlueDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = LaundryBlueDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = (stats?.totalTransactionsToday ?: 0).toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("Washes Done", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Divider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.height(40.dp).width(1.dp)
                    )
                    Column {
                        Text(
                            text = "%.1f".format(stats?.totalKgWashedToday ?: 0.0),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("Total Weight (kg)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // Recent entries logs list
            Text(
                "My Recent Entries",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LaundryBlueDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                val recent = transactions.take(5)
                if (recent.isEmpty()) {
                    Text(
                        "No laundry entries recorded today.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        recent.forEachIndexed { index, tx ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    tx.customerName ?: "Anonymous Customer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if ((tx.category ?: "laundry").lowercase() == "laundry") {
                                        "${tx.date} • Machine ${tx.machineNumber ?: "—"} • ${tx.weightKg ?: 0.0} kg • ${tx.soapUsedQty ?: 0.0} ${tx.soapProduct?.unit ?: ""} utilized"
                                    } else {
                                        tx.date
                                    },
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                if (tx.paymentMethod.lowercase() == "gcash") "GCash (${tx.referenceNumber})" else "Cash",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        val duration = calculateDurationInShop(tx.createdAt, tx.pickedUpAt)
                                        if (tx.pickedUp) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Claimed ($duration)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Unclaimed ($duration)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE65100),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!tx.pickedUp) {
                                        Button(
                                            onClick = {
                                                targetClaimTx = tx
                                                openClaimModal = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Claimed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                            if (index < recent.size - 1) {
                                Divider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Services selection dialog
    if (showServicesDialog) {
        AlertDialog(
            onDismissRequest = { showServicesDialog = false },
            title = { Text("Select Laundry Services", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        servicesList.forEach { s ->
                            val currentQty = selectedServices[s.id] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("₱${s.rate} / ${s.unit}", fontSize = 11.sp, color = Color.Gray)
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (currentQty > 0) {
                                                val next = currentQty - 1
                                                if (next == 0) selectedServices.remove(s.id)
                                                else selectedServices[s.id] = next
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null)
                                    }
                                    
                                    Text(
                                        currentQty.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            selectedServices[s.id] = currentQty + 1
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showServicesDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Modal Receipt popup Dialog
    if (showReceiptDialog && generatedReceipt != null) {
        val tx = generatedReceipt!!
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            title = { Text("Transaction Recorded Successfully", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("RECEIPT SUMMARY", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Divider()
                    
                    Text("Customer: ${tx.customerName ?: "Anonymous"}", fontSize = 12.sp)
                    Text("Date: ${tx.date}", fontSize = 12.sp)
                    if ((tx.category ?: "laundry").lowercase() == "laundry") {
                        Text("Machine: ${tx.machineNumber ?: "—"}", fontSize = 12.sp)
                        Text("Load Weight: ${tx.weightKg ?: 0.0} kg", fontSize = 12.sp)
                        Text("Resource Utilized: ${tx.soapUsedQty ?: 0.0} ${tx.soapProduct?.unit ?: ""}", fontSize = 12.sp)
                    }
                    Text("Payment channel: ${tx.paymentMethod}", fontSize = 12.sp)
                    if (tx.paymentMethod.lowercase() == "gcash") {
                        Text("GCash Ref: ${tx.referenceNumber}", fontSize = 12.sp)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("SERVICES BILLING BREAKDOWN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    tx.serviceItems?.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("- ${item.laundryService.name} (x${item.quantity})", fontSize = 11.sp)
                            Text("₱${"%.2f".format(item.priceAtTransaction * item.quantity)}", fontSize = 11.sp)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL AMOUNT PAID:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("₱${"%.2f".format(tx.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = LaundryBlue)
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!tx.pickedUp) {
                        Button(
                            onClick = {
                                targetClaimTx = tx
                                openClaimModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Claimed", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = { showReceiptDialog = false }) {
                        Text("Close Receipt")
                    }
                }
            }
        )
    }

    // Claim Confirmation Dialog with exact note requested
    if (openClaimModal && targetClaimTx != null) {
        val tx = targetClaimTx!!
        AlertDialog(
            onDismissRequest = {
                if (!isClaiming) {
                    openClaimModal = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Laundry Claim", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Transaction summary card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    tx.customerName ?: "Anonymous Customer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    currencyFormatter.format(tx.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LaundryBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Invoice #T-00${tx.id} • ${(tx.category ?: "laundry").replaceFirstChar { it.uppercase() }} • ${tx.date}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Green note box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Laundry will be marked as claimed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1B5E20),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "The transaction status will update to Claimed and its total amount will be inputted into sales.",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClaiming = true
                        viewModel.markTransactionAsPickedUp(tx.id) { success ->
                            isClaiming = false
                            if (success) {
                                openClaimModal = false
                                showReceiptDialog = false
                                targetClaimTx = null
                            }
                        }
                    },
                    enabled = !isClaiming,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Processing...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Yes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { openClaimModal = false },
                    enabled = !isClaiming,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("No", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
