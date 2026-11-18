package com.laundry.app

import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.laundry.app.ui.screens.*
import com.laundry.app.ui.theme.LaundryBlueDark
import com.laundry.app.ui.theme.LaundryInventorySystemTheme
import com.laundry.app.ui.theme.TealCyan
import com.laundry.app.viewmodel.LaundryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            LaundryInventorySystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainLayout()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Inventory Alerts"
            val descriptionText = "Notifications for low stock alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("inventory_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val adminOnly: Boolean = false) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.List)
    object Services : Screen("services", "Services & Rates", Icons.Default.LocalLaundryService)
    object Expenses : Screen("expenses", "Expenses Ledger", Icons.Default.Payment, adminOnly = true)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Inventory, adminOnly = true)
    object Branches : Screen("branches", "Branches Directory", Icons.Default.Store, adminOnly = true)
    object Reports : Screen("reports", "Reports Aggregates", Icons.Default.Assessment, adminOnly = true)
    object Users : Screen("users", "User Management", Icons.Default.People, adminOnly = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout() {
    val viewModel: LaundryViewModel = viewModel()
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    var showNotificationsDialog by remember { mutableStateOf(false) }
    val stats by viewModel.dashboardStats.collectAsState()
    val lowStocks = remember(stats) {
        stats?.soapStocks?.filter { it.isLow } ?: emptyList()
    }

    // Redirect to login if user is not authenticated and manage alerts
    val context = LocalContext.current
    var notifiedProductIds by remember { mutableStateOf(setOf<Long>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else {
            if (currentUser?.role == "ROLE_ADMIN") {
                viewModel.fetchDashboardStats(null, null)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            navController.navigate("dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    LaunchedEffect(lowStocks) {
        if (currentUser?.role == "ROLE_ADMIN") {
            val currentLowIds = lowStocks.map { it.id }.toSet()
            val newLowStocks = lowStocks.filter { it.id !in notifiedProductIds }
            if (newLowStocks.isNotEmpty()) {
                sendInventoryNotification(context, newLowStocks)
            }
            notifiedProductIds = currentLowIds
        }
    }

    // Navigation lists based on role
    val navigationItems = remember(currentUser) {
        val isAdmin = currentUser?.role == "ROLE_ADMIN"
        listOf(
            Screen.Dashboard,
            Screen.Transactions,
            Screen.Services,
            Screen.Expenses,
            Screen.Inventory,
            Screen.Branches,
            Screen.Reports,
            Screen.Users
        ).filter { !it.adminOnly || isAdmin }
    }

    val currentScreenTitle = remember(currentRoute) {
        listOf(
            Screen.Dashboard, Screen.Transactions, Screen.Services,
            Screen.Expenses, Screen.Inventory, Screen.Branches,
            Screen.Reports, Screen.Users
        ).find { it.route == currentRoute }?.title ?: "Tacky's Laundry"
    }

    if (currentUser == null) {
        // Render simple Login without Drawer scaffold
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(viewModel) {
                    // Success callback
                }
            }
        }
    } else {
        // Authenticated drawer wrap layout
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = LaundryBlueDark,
                    modifier = Modifier.width(280.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.tackys_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, TealCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Tacky's Laundry",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealCyan
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

                        // Scrollable drawer items
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            navigationItems.forEach { item ->
                                val isSelected = currentRoute == item.route
                                NavigationDrawerItem(
                                    label = { Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = TealCyan,
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                                        unselectedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Operator session info card
                        Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    currentUser?.fullName ?: "Operator",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    if (currentUser?.role == "ROLE_ADMIN") "Manager (Admin)" else "Employee Office",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Logout button
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(currentScreenTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (currentUser?.branchName != null) {
                                    Text(
                                        currentUser!!.branchName!!,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            if (currentUser?.role == "ROLE_ADMIN") {
                                Box(modifier = Modifier.padding(end = 8.dp)) {
                                    IconButton(onClick = { showNotificationsDialog = true }) {
                                        Icon(
                                            imageVector = if (lowStocks.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = if (lowStocks.isNotEmpty()) Color(0xFFBA1A1A) else LaundryBlueDark
                                        )
                                    }
                                    if (lowStocks.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.Red, CircleShape)
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = LaundryBlueDark,
                            navigationIconContentColor = LaundryBlueDark
                        )
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("login") {
                        LoginScreen(viewModel) {}
                    }
                    composable(Screen.Dashboard.route) {
                        val isAdmin = currentUser?.role == "ROLE_ADMIN"
                        if (isAdmin) {
                            AdminDashboardScreen(viewModel)
                        } else {
                            EmployeeDashboardScreen(viewModel) {
                                navController.navigate(Screen.Transactions.route)
                            }
                        }
                    }
                    composable(Screen.Transactions.route) {
                        TransactionsScreen(viewModel)
                    }
                    composable(Screen.Services.route) {
                        ServicesScreen(viewModel)
                    }
                    composable(Screen.Expenses.route) {
                        ExpensesScreen(viewModel)
                    }
                    composable(Screen.Inventory.route) {
                        InventoryScreen(viewModel)
                    }
                    composable(Screen.Branches.route) {
                        BranchesScreen(viewModel)
                    }
                    composable(Screen.Reports.route) {
                        ReportsScreen(viewModel)
                    }
                    composable(Screen.Users.route) {
                        UserManagementScreen(viewModel)
                    }
                }
            }

            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (lowStocks.isNotEmpty()) Icons.Default.Warning else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (lowStocks.isNotEmpty()) Color(0xFFE65100) else LaundryBlueDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notifications", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (lowStocks.isNotEmpty()) {
                                Text(
                                    "The following soap resources are running below alert levels. Please restock:",
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                
                                lowStocks.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFE65100)
                                            )
                                            Text(
                                                "${item.currentStock.run { "%.1f".format(this) }} / ${item.minStock.run { "%.1f".format(this) }} ${item.unit}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "No new notifications. All inventory levels are healthy.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showNotificationsDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

private fun sendInventoryNotification(context: Context, newLowStocks: List<com.laundry.app.api.SoapStockStatus>) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    val itemsText = newLowStocks.joinToString(", ") { "${it.name} (${it.currentStock} ${it.unit} left)" }
    val title = "Low Inventory Alert!"
    val content = "The following items are running low: $itemsText. Please restock soon."

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 
        0, 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, "inventory_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    notificationManager.notify(1001, builder.build())
}
