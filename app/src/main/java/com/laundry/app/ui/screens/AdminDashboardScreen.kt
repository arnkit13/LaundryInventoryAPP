package com.laundry.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundry.app.api.BreakdownItem
import com.laundry.app.viewmodel.LaundryViewModel
import com.laundry.app.ui.theme.LaundryBlue
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.TealCyan
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

private val CHART_COLORS = listOf(
    Color(0xFF0B5394), Color(0xFF00BCD4), Color(0xFF8FCE00), Color(0xFFFFD666),
    Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFFFF9800),
    Color(0xFF795548), Color(0xFF607D8B)
)

@Composable
fun AdminDashboardScreen(viewModel: LaundryViewModel) {
    var filterType by remember { mutableStateOf("monthly") }
    
    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    var selectedBranchId by remember { mutableStateOf<Long?>(null) }

    val stats by viewModel.dashboardStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { scrollState.value > 100 }
    }

    val monthsList = listOf(
        1 to "January", 2 to "February", 3 to "March", 4 to "April",
        5 to "May", 6 to "June", 7 to "July", 8 to "August",
        9 to "September", 10 to "October", 11 to "November", 12 to "December"
    )
    val yearsList = listOf(2024, 2025, 2026, 2027, 2028)

    LaunchedEffect(filterType, selectedMonth, selectedYear, selectedBranchId) {
        val monthParam = if (filterType == "annual") 0 else selectedMonth
        viewModel.fetchDashboardStats(selectedYear, monthParam, selectedBranchId)
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
        if (isLoading && stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
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

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            TextButton(
                                onClick = { filterType = "monthly" },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (filterType == "monthly") MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (filterType == "monthly") Color.White else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("Monthly Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { filterType = "annual" },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (filterType == "annual") MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (filterType == "annual") Color.White else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("Annual Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (filterType == "monthly") {
                                Box(modifier = Modifier.weight(1.3f)) {
                                    MonthSpinner(
                                        selectedMonth = selectedMonth,
                                        monthsList = monthsList,
                                        onMonthSelected = { selectedMonth = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                YearSpinner(
                                    selectedYear = selectedYear,
                                    yearsList = yearsList,
                                    onYearSelected = { selectedYear = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        var branchExpanded by remember { mutableStateOf(false) }
                        val branches by viewModel.branches.collectAsState()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = branches.find { it.id == selectedBranchId }?.name ?: "All Branches",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Filter by Branch") },
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

                errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = if (filterType == "monthly") {
                        "${monthsList.find { it.first == selectedMonth }?.second?.uppercase()} $selectedYear"
                    } else {
                        "ANNUAL REPORT $selectedYear"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF073763),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Tacky's Laundry • Shop Manager Dashboard",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Today's Overview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF073763),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currencyFormatter.format(stats?.totalRevenueToday ?: 0.0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF33691E) // Greenish color matching income
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Revenue", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = (stats?.totalTransactionsToday ?: 0).toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF073763)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Transactions", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${"%.1f".format(stats?.totalKgWashedToday ?: 0.0)} kg",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF073763)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Washed Weight", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
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
                            title = "INCOME",
                            value = currencyFormatter.format(stats?.totalRevenue ?: 0.0),
                            bgColor = Color(0xFFF1F8E9),
                            borderColor = Color(0xFFC5E1A5),
                            textColor = Color(0xFF33691E),
                            modifier = Modifier.weight(1f)
                        )

                        MetricCard(
                            title = "EXPENSES",
                            value = currencyFormatter.format(stats?.totalExpenses ?: 0.0),
                            bgColor = Color(0xFFFDE8E8),
                            borderColor = Color(0xFFF8B4B4),
                            textColor = Color(0xFFC81E1E),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val netProfit = stats?.netProfit ?: 0.0
                    MetricCard(
                        title = "PROFIT",
                        value = currencyFormatter.format(netProfit),
                        bgColor = if (netProfit >= 0) Color(0xFFE0F2F1) else Color(0xFFFDE8E8),
                        borderColor = if (netProfit >= 0) Color(0xFF80CBC4) else Color(0xFFF8B4B4),
                        textColor = if (netProfit >= 0) Color(0xFF004D40) else Color(0xFFC81E1E),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Analytical Breakdowns",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF073763),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                stats?.let { statsData ->
                    DonutChartCard(
                        title = "MONTHLY EXPENSES",
                        data = statsData.expenseCategoryBreakdown ?: emptyList(),
                        currencyFormatter = currencyFormatter
                    )

                    DonutChartCard(
                        title = "MONTHLY INCOME BY SERVICE",
                        data = statsData.incomeServiceBreakdown ?: emptyList(),
                        currencyFormatter = currencyFormatter
                    )

                    DonutChartCard(
                        title = "PAYMENT CHANNEL REVENUE (MOP)",
                        data = statsData.mopBreakdown ?: emptyList(),
                        currencyFormatter = currencyFormatter
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                                TableTabsSection(statsData = stats, currencyFormatter = currencyFormatter)
            }
        }

        if (showScrollToTop) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll to top")
            }
        }
    }
}

@Composable
fun MonthSpinner(
    selectedMonth: Int,
    monthsList: List<Pair<Int, String>>,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = monthsList.find { it.first == selectedMonth }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Month") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(8.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
            monthsList.forEach { (num, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = Color.Black) },
                    onClick = {
                        onMonthSelected(num)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun YearSpinner(
    selectedYear: Int,
    yearsList: List<Int>,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedYear.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Year") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(8.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
            yearsList.forEach { y ->
                DropdownMenuItem(
                    text = { Text(y.toString(), color = Color.Black) },
                    onClick = {
                        onYearSelected(y)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

@Composable
fun DonutChartCard(
    title: String,
    data: List<BreakdownItem>,
    currencyFormatter: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF073763),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available for this section.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val total = data.sumOf { it.value }
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            data.forEachIndexed { index, item ->
                                val sweepAngle = if (total > 0) (item.value / total * 360).toFloat() else 0f
                                drawArc(
                                    color = CHART_COLORS[index % CHART_COLORS.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx())
                                )
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currencyFormatter.format(total),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF073763)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.forEachIndexed { index, item ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                CHART_COLORS[index % CHART_COLORS.size],
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        maxLines = 1
                                    )
                                }
                                val percentage = if (total > 0) (item.value / total * 100) else 0.0
                                Text(
                                    text = "${currencyFormatter.format(item.value)} (${"%.1f".format(percentage)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableTabsSection(
    statsData: com.laundry.app.api.DashboardStats?,
    currencyFormatter: NumberFormat
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Revenue by Date", "Expense Categories", "Income by Service")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = LaundryBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = LaundryBlue,
                        unselectedContentColor = Color.Gray,
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            val items = when (selectedTab) {
                0 -> statsData?.incomeByDate ?: emptyList()
                1 -> statsData?.expenseByCategory ?: emptyList()
                else -> statsData?.incomeByService ?: emptyList()
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tabular logs available for this period.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category / Date", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = LaundryBlueDark)
                        Text("Total Amount", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = LaundryBlueDark)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                            Text(currencyFormatter.format(item.value), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Divider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

