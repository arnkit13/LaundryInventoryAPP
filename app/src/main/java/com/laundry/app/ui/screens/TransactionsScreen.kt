package com.laundry.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.app.api.LaundryTransaction
import com.laundry.app.api.TransactionRequest
import com.laundry.app.api.ServiceItemRequest
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
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
fun TransactionsScreen(viewModel: LaundryViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val products by viewModel.products.collectAsState()
    val servicesList by viewModel.services.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Claim Status Filter: "unclaimed" (default) | "claimed" | "all"
    var claimFilter by remember { mutableStateOf("unclaimed") }

    // Claim Confirmation Dialog State
    var openClaimModal by remember { mutableStateOf(false) }
    var targetClaimTx by remember { mutableStateOf<LaundryTransaction?>(null) }
    var isClaiming by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedBranchId by remember { mutableStateOf<Long?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("All") }
    
    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf<Int?>(calendar.get(Calendar.MONTH) + 1) } // null means "All Months"
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var filtersExpanded by remember { mutableStateOf(false) }

    var selectedTransaction by remember { mutableStateOf<LaundryTransaction?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editDate by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("laundry") }
    var editCustomerName by remember { mutableStateOf("") }
    var editWeightKg by remember { mutableStateOf("") }
    var editMachineNumber by remember { mutableStateOf("") }
    var editSelectedProductId by remember { mutableStateOf<Long?>(null) }
    var editSoapUsedQty by remember { mutableStateOf("") }
    var editPaymentMethod by remember { mutableStateOf("Cash") }
    var editReferenceNumber by remember { mutableStateOf("") }
    val editSelectedServices = remember { mutableStateMapOf<Long, Int>() }
    var editShowServicesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showEditDialog) {
        if (showEditDialog) {
            val tx = selectedTransaction
            if (tx != null) {
                editDate = tx.date
                editCategory = tx.category ?: "laundry"
                editCustomerName = tx.customerName ?: ""
                editWeightKg = tx.weightKg?.toString() ?: ""
                editMachineNumber = tx.machineNumber ?: ""
                editSelectedProductId = tx.soapProduct?.id
                editSoapUsedQty = tx.soapUsedQty?.toString() ?: ""
                editPaymentMethod = tx.paymentMethod
                editReferenceNumber = tx.referenceNumber ?: ""
                editSelectedServices.clear()
                tx.serviceItems?.forEach { item ->
                    editSelectedServices[item.laundryService.id] = item.quantity
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchTransactions()
        viewModel.fetchBranches()
        viewModel.fetchInventory()
        viewModel.fetchServices()
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    val isAdmin = currentUser?.role == "ROLE_ADMIN"

    val unclaimedCount = remember(transactions) { transactions.count { !it.pickedUp } }
    val claimedCount = remember(transactions) { transactions.count { it.pickedUp } }

    // Filter transactions locally
    val filteredTransactions = transactions.filter { tx ->
        // Claim status filter
        if (claimFilter == "unclaimed" && tx.pickedUp) return@filter false
        if (claimFilter == "claimed" && !tx.pickedUp) return@filter false

        val matchesSearch = tx.customerName?.contains(searchQuery, ignoreCase = true) ?: searchQuery.isEmpty()
        val matchesBranch = if (isAdmin && selectedBranchId != null) tx.branch?.id == selectedBranchId else true
        val matchesPayment = if (selectedPaymentMethod != "All") tx.paymentMethod.lowercase() == selectedPaymentMethod.lowercase() else true
        
        val matchesMonthYear = try {
            val parts = tx.date.split("-")
            val txYear = parts[0].toInt()
            val txMonth = parts[1].toInt()
            val matchesY = txYear == selectedYear
            val matchesM = if (selectedMonth != null) txMonth == selectedMonth else true
            matchesY && matchesM
        } catch (e: Exception) {
            false
        }

        matchesSearch && matchesBranch && matchesPayment && matchesMonthYear
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Transactions History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaundryBlueDark
                )
                IconButton(onClick = { viewModel.fetchTransactions() }) {
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

            // Claimed and Unclaimed Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Unclaimed Button
                Button(
                    onClick = { claimFilter = "unclaimed" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (claimFilter == "unclaimed") Color(0xFFF57F17) else Color.White,
                        contentColor = if (claimFilter == "unclaimed") Color.White else Color(0xFFE65100)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .border(
                            1.dp,
                            if (claimFilter == "unclaimed") Color(0xFFF57F17) else Color(0xFFFFCC80),
                            RoundedCornerShape(10.dp)
                        ),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (claimFilter == "unclaimed") 2.dp else 0.dp)
                ) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Unclaimed ($unclaimedCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Claimed Button
                Button(
                    onClick = { claimFilter = "claimed" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (claimFilter == "claimed") Color(0xFF2E7D32) else Color.White,
                        contentColor = if (claimFilter == "claimed") Color.White else Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .border(
                            1.dp,
                            if (claimFilter == "claimed") Color(0xFF2E7D32) else Color(0xFFA5D6A7),
                            RoundedCornerShape(10.dp)
                        ),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (claimFilter == "claimed") 2.dp else 0.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Claimed ($claimedCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // All Button
                Button(
                    onClick = { claimFilter = "all" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (claimFilter == "all") LaundryBlueDark else Color.White,
                        contentColor = if (claimFilter == "all") Color.White else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .border(
                            1.dp,
                            if (claimFilter == "all") LaundryBlueDark else Color(0xFFCCCCCC),
                            RoundedCornerShape(10.dp)
                        ),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (claimFilter == "all") 2.dp else 0.dp)
                ) {
                    Text(
                        "All (${transactions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (claimFilter == "unclaimed") {
                Text(
                    "* Active orders awaiting pickup. Orders unclaimed for 2+ weeks are automatically marked as claimed.",
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Compact Collapsible Filters Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                val dropdownFieldColors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color(0xFFCCCCCC),
                    disabledLabelColor = Color.DarkGray,
                    disabledTrailingIconColor = Color.DarkGray,
                    disabledContainerColor = Color.Transparent
                )

                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customer...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Button(
                            onClick = { filtersExpanded = !filtersExpanded },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (filtersExpanded) LaundryBlue else Color(0xFFF1F5F9),
                                contentColor = if (filtersExpanded) Color.White else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Toggle Filters",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (filtersExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Filters Grid
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Branch (Admin only) & Payment Method
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isAdmin) {
                                    var branchExpanded by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { branchExpanded = true }
                                    ) {
                                        OutlinedTextField(
                                            value = branches.find { it.id == selectedBranchId }?.name ?: "All Branches",
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = false,
                                            colors = dropdownFieldColors,
                                            label = { Text("Branch") },
                                            trailingIcon = {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
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

                                var paymentExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { paymentExpanded = true }
                                ) {
                                    OutlinedTextField(
                                        value = selectedPaymentMethod,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        colors = dropdownFieldColors,
                                        label = { Text("Payment") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    DropdownMenu(
                                        expanded = paymentExpanded,
                                        onDismissRequest = { paymentExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        listOf("All", "Cash", "GCash").forEach { pm ->
                                            DropdownMenuItem(
                                                text = { Text(pm, color = Color.Black) },
                                                onClick = {
                                                    selectedPaymentMethod = pm
                                                    paymentExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 2: Month & Year Filters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var monthExpanded by remember { mutableStateOf(false) }
                                val monthsList = listOf(
                                    null to "All Months", 1 to "January", 2 to "February", 3 to "March", 4 to "April",
                                    5 to "May", 6 to "June", 7 to "July", 8 to "August",
                                    9 to "September", 10 to "October", 11 to "November", 12 to "December"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clickable { monthExpanded = true }
                                ) {
                                    OutlinedTextField(
                                        value = monthsList.find { it.first == selectedMonth }?.second ?: "All Months",
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        colors = dropdownFieldColors,
                                        label = { Text("Month") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    DropdownMenu(
                                        expanded = monthExpanded,
                                        onDismissRequest = { monthExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        monthsList.forEach { (num, name) ->
                                            DropdownMenuItem(
                                                text = { Text(name, color = Color.Black) },
                                                onClick = {
                                                    selectedMonth = num
                                                    monthExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                var yearExpanded by remember { mutableStateOf(false) }
                                val yearsList = listOf(2024, 2025, 2026, 2027, 2028)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { yearExpanded = true }
                                ) {
                                    OutlinedTextField(
                                        value = selectedYear.toString(),
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        colors = dropdownFieldColors,
                                        label = { Text("Year") },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    DropdownMenu(
                                        expanded = yearExpanded,
                                        onDismissRequest = { yearExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        yearsList.forEach { y ->
                                            DropdownMenuItem(
                                                text = { Text(y.toString(), color = Color.Black) },
                                                onClick = {
                                                    selectedYear = y
                                                    yearExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results lists
            if (isLoading && transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions matching your criteria.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredTransactions.forEach { tx ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.clearFeedback()
                                    selectedTransaction = tx
                                    showDetailDialog = true
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        tx.customerName ?: "Anonymous Customer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        currencyFormatter.format(tx.totalAmount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = LaundryBlue
                                    )
                                }
                                
                                 Text(
                                    if ((tx.category ?: "laundry").lowercase() == "laundry") {
                                        "${tx.date} • ${tx.machineNumber ?: "No Machine"} • ${tx.weightKg ?: 0.0} kg washed"
                                    } else {
                                        tx.date
                                    },
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category Badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when ((tx.category ?: "laundry").lowercase()) {
                                                        "gym" -> Color(0xFFE8F5E9)
                                                        "pickleball" -> Color(0xFFFFF3E0)
                                                        else -> Color(0xFFE1F5FE)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                (tx.category ?: "laundry").replaceFirstChar { it.uppercase() },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when ((tx.category ?: "laundry").lowercase()) {
                                                    "gym" -> Color(0xFF2E7D32)
                                                    "pickleball" -> Color(0xFFE65100)
                                                    else -> LaundryBlue
                                                }
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (tx.paymentMethod.lowercase() == "gcash") "GCash (${tx.referenceNumber})" else "Cash",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                        }
                                        if (tx.branch != null) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F4FC), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    tx.branch.name,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LaundryBlue
                                                )
                                            }
                                        }
                                        val duration = calculateDurationInShop(tx.createdAt, tx.pickedUpAt)
                                        if (tx.pickedUp) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Claimed ($duration)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Unclaimed ($duration)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
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

                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Details",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
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

    // Modal Details dialog
    if (showDetailDialog && selectedTransaction != null) {
        val tx = selectedTransaction!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transaction ID:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("#${tx.id}", fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text((tx.category ?: "laundry").replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(tx.date, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Customer:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(tx.customerName ?: "Anonymous", fontSize = 12.sp)
                    }
                    if ((tx.category ?: "laundry").lowercase() == "laundry") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Weight:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${tx.weightKg ?: 0.0} kg", fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Soap Product:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(tx.soapProduct?.name ?: "—", fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Soap Consumed:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${tx.soapUsedQty ?: 0.0} ${tx.soapProduct?.unit ?: ""}", fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Stock Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${tx.soapRemainingQty ?: 0.0} ${tx.soapProduct?.unit ?: ""}", fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Machine allocation:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(tx.machineNumber ?: "—", fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recorded By:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(tx.user?.fullName ?: "System User", fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Branch:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(tx.branch?.name ?: "Shared / Global", fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MOP Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(tx.paymentMethod, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pickup Status:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val duration = calculateDurationInShop(tx.createdAt, tx.pickedUpAt)
                        Text(
                            if (tx.pickedUp) "Claimed ($duration)" else "Unclaimed ($duration)",
                            fontWeight = FontWeight.Bold,
                            color = if (tx.pickedUp) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontSize = 12.sp
                        )
                    }
                    if (tx.paymentMethod.lowercase() == "gcash") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GCash Reference:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(tx.referenceNumber ?: "—", fontSize = 12.sp)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Services Breakdown", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    tx.serviceItems?.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("- ${item.laundryService.name} (x${item.quantity})", fontSize = 11.sp)
                            Text(currencyFormatter.format(item.priceAtTransaction * item.quantity), fontSize = 11.sp)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL BILLING PRICE:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(currencyFormatter.format(tx.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = LaundryBlue)
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showDetailDialog = false
                            showEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LaundryBlue)
                    ) {
                        Text("Edit", color = Color.White)
                    }
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
                    Button(onClick = { showDetailDialog = false }) {
                        Text("Close")
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
                                showDetailDialog = false
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

    // Modal Edit transaction dialog
    if (showEditDialog && selectedTransaction != null) {
        val tx = selectedTransaction!!
        val context = LocalContext.current
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Transaction #${tx.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date
                    val calendar = Calendar.getInstance()
                    val datePickerDialog = remember {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedMonth = String.format(Locale.US, "%02d", month + 1)
                                val formattedDay = String.format(Locale.US, "%02d", dayOfMonth)
                                editDate = "$year-$formattedMonth-$formattedDay"
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }

                    OutlinedTextField(
                        value = editDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction Date *") },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Category
                    Text("Transaction Category *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("laundry", "Gym", "pickleball").forEach { cat ->
                            val isSelected = editCategory == cat
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LaundryBlue else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                                    .clickable { 
                                        editCategory = cat
                                        if (cat != "laundry") {
                                            editSelectedProductId = null
                                            editSoapUsedQty = ""
                                            editWeightKg = ""
                                        }
                                    }
                            ) {
                                Text(
                                    text = cat.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            }
                        }
                    }

                    // Customer Name
                    OutlinedTextField(
                        value = editCustomerName,
                        onValueChange = { editCustomerName = it },
                        label = { Text("Customer's Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Laundry specifics
                    if (editCategory == "laundry") {
                        OutlinedTextField(
                            value = editWeightKg,
                            onValueChange = { editWeightKg = it },
                            label = { Text("Weight (kg)") },
                            suffix = { Text("kg") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = editMachineNumber,
                            onValueChange = { editMachineNumber = it },
                            label = { Text("Washing Machine Unit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Soap selection
                        var soapExpanded by remember { mutableStateOf(false) }
                        val selectedProduct = products.find { it.id == editSelectedProductId }
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
                                        text = { Text("${p.name} (${p.quantity} left)", color = Color.Black) },
                                        onClick = {
                                            editSelectedProductId = p.id
                                            soapExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editSoapUsedQty,
                            onValueChange = { editSoapUsedQty = it },
                            label = { Text("Soap Quantity Utilized") },
                            suffix = { Text(selectedProduct?.unit ?: "unit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Services dialog selector trigger
                    Button(
                        onClick = { editShowServicesDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = LaundryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, LaundryBlue, RoundedCornerShape(8.dp))
                    ) {
                        Text("Choose Services (${editSelectedServices.size} Selected)", fontWeight = FontWeight.Bold)
                    }

                    // MOP
                    Text("Mode of Payment *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Cash", "Gcash").forEach { mop ->
                            val isSelected = editPaymentMethod == mop
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LaundryBlue else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                                    .clickable { editPaymentMethod = mop }
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

                    if (editPaymentMethod.lowercase() == "gcash") {
                        OutlinedTextField(
                            value = editReferenceNumber,
                            onValueChange = { editReferenceNumber = it },
                            label = { Text("Last 4 digits of GCash Ref") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val request = TransactionRequest(
                            date = editDate,
                            customerName = editCustomerName,
                            weightKg = if (editCategory == "laundry") (editWeightKg.toDoubleOrNull() ?: 0.0) else null,
                            soapProductId = if (editCategory == "laundry") editSelectedProductId else null,
                            soapUsedQty = if (editCategory == "laundry") (editSoapUsedQty.toDoubleOrNull() ?: 0.0) else null,
                            machineNumber = if (editCategory == "laundry") editMachineNumber else null,
                            paymentMethod = editPaymentMethod,
                            referenceNumber = editReferenceNumber,
                            services = editSelectedServices.map { (id, qty) ->
                                ServiceItemRequest(id, qty)
                            },
                            category = editCategory
                        )
                        viewModel.updateTransaction(tx.id, request) { success ->
                            if (success) {
                                showEditDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Edit Services selection dialog
    if (editShowServicesDialog) {
        AlertDialog(
            onDismissRequest = { editShowServicesDialog = false },
            title = { Text("Select Services", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        servicesList.forEach { s ->
                            val currentQty = editSelectedServices[s.id] ?: 0
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
                                                if (next == 0) editSelectedServices.remove(s.id)
                                                else editSelectedServices[s.id] = next
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
                                            editSelectedServices[s.id] = currentQty + 1
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
                Button(onClick = { editShowServicesDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
