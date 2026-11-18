package com.laundry.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.laundry.app.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class LaundryRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("laundry_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var useMock: Boolean
        get() = prefs.getBoolean("use_mock", false)
        set(value) = prefs.edit().putBoolean("use_mock", value).apply()

    init {
        val savedUrl = prefs.getString("base_url", null)
        if (savedUrl != null) {
            RetrofitClient.setBaseUrl(savedUrl)
        }
    }

    var baseUrl: String
        get() = prefs.getString("base_url", "https://front-back-node-laundry-system-production.up.railway.app/") ?: "https://front-back-node-laundry-system-production.up.railway.app/"
        set(value) = prefs.edit().putString("base_url", value).apply()

    fun saveSession(user: JwtResponse) {
        prefs.edit().putString("current_user", gson.toJson(user)).apply()
        RetrofitClient.setToken(user.token)
    }

    fun getSession(): JwtResponse? {
        val json = prefs.getString("current_user", null) ?: return null
        return try {
            val user = gson.fromJson(json, JwtResponse::class.java)
            RetrofitClient.setToken(user.token)
            user
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        prefs.edit().remove("current_user").apply()
        RetrofitClient.setToken(null)
    }

    private suspend fun <T> executeCall(call: Call<T>): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else if (response.code() == 200 || response.code() == 204) {
                    @Suppress("UNCHECKED_CAST")
                    val emptyValue = try {
                        null as T
                    } catch (e: Exception) {
                        Unit as T
                    }
                    Result.success(emptyValue)
                } else {
                    Result.failure(IOException("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val message = extractErrorMessage(errorBody) ?: "HTTP Error ${response.code()}: ${response.message()}"
                Result.failure(Exception(message))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error, please check connection. ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(json: String?): String? {
        if (json.isNullOrEmpty()) return null
        return try {
            val map = gson.fromJson(json, Map::class.java)
            map["message"]?.toString() ?: map["error"]?.toString()
        } catch (e: Exception) {
            json
        }
    }

    companion object {
        private val mockBranches = mutableListOf<Branch>().apply {
            add(Branch(1, "Main Branch", "123 Central Avenue, Quezon City", "Machine 1:true,Machine 2:true,Machine 3:true,Machine 4:true", "2026-07-01T12:00:00Z"))
            add(Branch(2, "Downtown Express", "456 Mabini St, Manila", "Machine 1:true,Machine 2:true,Machine 3:true,Machine 4:true", "2026-07-02T12:00:00Z"))
        }

        private val mockUsers = mutableListOf<User>().apply {
            add(User(1, "admin", "Shop Manager (Admin)", "ROLE_ADMIN", true, mockBranches[0]))
            add(User(2, "employee", "Juan Dela Cruz (Employee)", "ROLE_USER", true, mockBranches[1]))
        }
        
        private val mockUserPasswords = mutableMapOf("admin" to "admin123", "employee" to "emp123")

        private val mockServices = mutableListOf<LaundryService>().apply {
            add(LaundryService(1, "Basic Service", 180.0, "service"))
            add(LaundryService(2, "Comforter", 180.0, "pc"))
            add(LaundryService(3, "Extra Wash", 20.0, "wash"))
            add(LaundryService(4, "Extra Rinse", 20.0, "rinse"))
            add(LaundryService(5, "Extra Dry", 30.0, "dry"))
            add(LaundryService(6, "Extra Kilo", 20.0, "kilo"))
            add(LaundryService(7, "Detergent", 10.0, "sachet"))
            add(LaundryService(8, "SELF SERVICE", 100.0, "LOAD"))
            add(LaundryService(9, "FREE", 0.0, "LOAD"))
            add(LaundryService(10, "BEDSHEETS", 180.0, ""))
            add(LaundryService(11, "SOFTENER", 10.0, ""))
            add(LaundryService(12, "ICE", 15.0, "PACK"))
        }

        private val mockProducts = mutableListOf<SoapProduct>().apply {
            add(SoapProduct(1, "Powder Detergent (Lemon Clean)", 24.5, "kg", 5.0, 30.0))
            add(SoapProduct(2, "Liquid Detergent (Ocean Fresh)", 4.2, "L", 5.0, 20.0))
            add(SoapProduct(3, "Fabric Softener (Lavender Mist)", 15.0, "L", 5.0, 15.0))
        }

        private val mockHistory = mutableListOf<SoapInventoryHistory>().apply {
            add(SoapInventoryHistory(1, "2026-07-31T10:00:00Z", SoapProductShort(1, "Powder Detergent (Lemon Clean)", "kg"), "ADD_STOCK", 30.0, 0.0, 30.0, UserShort(1, "Shop Manager (Admin)"), "Initial stock setup"))
            add(SoapInventoryHistory(2, "2026-07-31T10:05:00Z", SoapProductShort(2, "Liquid Detergent (Ocean Fresh)", "L"), "ADD_STOCK", 20.0, 0.0, 20.0, UserShort(1, "Shop Manager (Admin)"), "Initial stock setup"))
            add(SoapInventoryHistory(3, "2026-07-31T10:10:00Z", SoapProductShort(3, "Fabric Softener (Lavender Mist)", "L"), "ADD_STOCK", 15.0, 0.0, 15.0, UserShort(1, "Shop Manager (Admin)"), "Initial stock setup"))
        }

        private val mockTransactions = mutableListOf<LaundryTransaction>().apply {
            val now = System.currentTimeMillis()
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val dateOnlyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            val twoHoursAgo = now - 2 * 60 * 60 * 1000
            val createdAt1 = formatter.format(Date(twoHoursAgo))
            val date1 = dateOnlyFormatter.format(Date(twoHoursAgo))
            
            val oneDayAgo = now - 24 * 60 * 60 * 1000
            val fourHoursLater = oneDayAgo + 4 * 60 * 60 * 1000
            val createdAt2 = formatter.format(Date(oneDayAgo))
            val pickedUpAt2 = formatter.format(Date(fourHoursLater))
            val date2 = dateOnlyFormatter.format(Date(oneDayAgo))
            add(
                LaundryTransaction(
                    id = 1L,
                    date = date1,
                    customerName = "Juan Dela Cruz",
                    weightKg = 7.5,
                    soapProduct = SoapProductShort(1, "Powder Detergent (Lemon Clean)", "kg"),
                    soapUsedQty = 0.5,
                    soapRemainingQty = 29.5,
                    user = UserShort(2, "Operator Jane"),
                    machineNumber = "M-02",
                    branch = Branch(1, "Main Branch City", "123 Main St", "09   17-123-4567"),
                    paymentMethod = "Cash",
                    referenceNumber = null,
                    totalAmount = 185.00,
                    createdAt = createdAt1,
                    serviceItems = listOf(
                        TransactionServiceItem(1.0, LaundryService(1, "Regular Wash & Dry", 80.0, "kg"), 2, 80.0),
                        TransactionServiceItem(2.0, LaundryService(2, "Fabric Conditioning", 25.0, "pcs"), 1, 25.0)
                    ),
                    pickedUp = false,
                    pickedUpAt = null
                )
            )

            add(
                LaundryTransaction(
                    id = 2L,
                    date = date2,
                    customerName = "Maria Clara",
                    weightKg = 5.0,
                    soapProduct = SoapProductShort(2, "Liquid Detergent (Ocean Fresh)", "L"),
                    soapUsedQty = 0.3,
                    soapRemainingQty = 19.7,
                    user = UserShort(2, "Operator Jane"),
                    machineNumber = "M-01",
                    branch = Branch(1, "Main Branch City", "123 Main St", "0917-123-4567"),
                    paymentMethod = "GCash",
                    referenceNumber = "8899",
                    totalAmount = 120.00,
                    createdAt = createdAt2,
                    serviceItems = listOf(
                        TransactionServiceItem(3.0, LaundryService(1, "Regular Wash & Dry", 80.0, "kg"), 1, 80.0),
                        TransactionServiceItem(4.0, LaundryService(3, "Express Wash", 40.0, "kg"), 1, 40.0)
                    ),
                    pickedUp = true,
                    pickedUpAt = pickedUpAt2
                )
            )
        }
        private val mockExpenses = mutableListOf<Expense>()
    }

    private suspend fun mockDelay() {
        withContext(Dispatchers.Default) {
            Thread.sleep(400)
        }
    }

    suspend fun login(request: LoginRequest): Result<JwtResponse> {
        if (useMock) {
            mockDelay()
            val matchedUser = mockUsers.find { it.username == request.username }
            val matchedPassword = mockUserPasswords[request.username]
            if (matchedUser == null || matchedPassword != request.password) {
                return Result.failure(Exception("Invalid username or password."))
            }
            if (!matchedUser.active) {
                return Result.failure(Exception("Account is disabled. Please contact the administrator."))
            }
            val mockJwt = JwtResponse(
                token = "mock-jwt-token-for-" + matchedUser.username,
                id = matchedUser.id,
                username = matchedUser.username,
                fullName = matchedUser.fullName,
                role = matchedUser.role,
                branchName = matchedUser.branch?.name,
                branchId = matchedUser.branch?.id
            )
            return Result.success(mockJwt)
        }
        return executeCall(RetrofitClient.apiService.login(request))
    }

    suspend fun getBranches(): Result<List<Branch>> {
        if (useMock) {
            mockDelay()
            return Result.success(mockBranches.toList())
        }
        return executeCall(RetrofitClient.apiService.getBranches())
    }

    suspend fun createBranch(request: BranchRequest): Result<Branch> {
        if (useMock) {
            mockDelay()
            if (request.name.isEmpty() || request.location.isEmpty()) {
                return Result.failure(Exception("Name and Location are required."))
            }
            val newId = if (mockBranches.isEmpty()) 1 else mockBranches.maxOf { it.id } + 1
            val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val branch = Branch(newId, request.name, request.location, request.machinesConfig, dateStr)
            mockBranches.add(branch)
            return Result.success(branch)
        }
        return executeCall(RetrofitClient.apiService.createBranch(request))
    }

    suspend fun updateBranch(id: Long, request: BranchRequest): Result<Branch> {
        if (useMock) {
            mockDelay()
            val index = mockBranches.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Branch not found."))
            val oldBranch = mockBranches[index]
            val updatedBranch = oldBranch.copy(
                name = request.name,
                location = request.location,
                machinesConfig = request.machinesConfig
            )
            mockBranches[index] = updatedBranch
            return Result.success(updatedBranch)
        }
        return executeCall(RetrofitClient.apiService.updateBranch(id, request))
    }

    suspend fun deleteBranch(id: Long): Result<Unit> {
        if (useMock) {
            mockDelay()
            val index = mockBranches.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Branch not found."))
            mockBranches.removeAt(index)
            
            for (i in mockUsers.indices) {
                if (mockUsers[i].branch?.id == id) {
                    mockUsers[i] = mockUsers[i].copy(branch = null)
                }
            }
            for (i in mockTransactions.indices) {
                if (mockTransactions[i].branch?.id == id) {
                    mockTransactions[i] = mockTransactions[i].copy(branch = null)
                }
            }
            return Result.success(Unit)
        }
        return executeCall(RetrofitClient.apiService.deleteBranch(id)).map { Unit }
    }

    suspend fun getServices(): Result<List<LaundryService>> {
        if (useMock) {
            mockDelay()
            return Result.success(mockServices)
        }
        return executeCall(RetrofitClient.apiService.getServices())
    }

    suspend fun createService(request: LaundryServiceRequest): Result<LaundryService> {
        if (useMock) {
            mockDelay()
            if (mockServices.any { it.name.equals(request.name, ignoreCase = true) }) {
                return Result.failure(Exception("Service name already exists."))
            }
            val newId = if (mockServices.isEmpty()) 1 else mockServices.maxOf { it.id } + 1
            val service = LaundryService(newId, request.name, request.rate, request.unit)
            mockServices.add(service)
            return Result.success(service)
        }
        return executeCall(RetrofitClient.apiService.createService(request))
    }

    suspend fun updateService(id: Long, request: LaundryServiceRequest): Result<LaundryService> {
        if (useMock) {
            mockDelay()
            val index = mockServices.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Service not found."))
            if (mockServices.any { it.id != id && it.name.equals(request.name, ignoreCase = true) }) {
                return Result.failure(Exception("Service name already exists."))
            }
            val service = LaundryService(id, request.name, request.rate, request.unit)
            mockServices[index] = service
            return Result.success(service)
        }
        return executeCall(RetrofitClient.apiService.updateService(id, request))
    }

    suspend fun deleteService(id: Long): Result<Unit> {
        if (useMock) {
            mockDelay()
            val index = mockServices.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Service not found."))
            mockServices.removeAt(index)
            return Result.success(Unit)
        }
        return executeCall(RetrofitClient.apiService.deleteService(id)).map { Unit }
    }

    suspend fun getStats(year: Int?, month: Int?, branchId: Long? = null): Result<DashboardStats> {
        if (useMock) {
            mockDelay()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            
            val todayTransactions = mockTransactions.filter { 
                it.date == todayStr && (branchId == null || it.branch?.id == branchId)
            }
            val totalTransactionsToday = todayTransactions.size.toLong()
            val totalKgWashedToday = todayTransactions.sumOf { it.weightKg ?: 0.0 }
            val totalCustomersToday = todayTransactions.mapNotNull { it.customerName?.trim() }.filter { it.isNotEmpty() }.distinct().size.toLong()
                .let { if (it == 0L) totalTransactionsToday else it }
            val totalRevenueToday = todayTransactions.filter { it.pickedUp }.sumOf { it.totalAmount }

            val soapStocks = mockProducts.map { p ->
                SoapStockStatus(
                    id = p.id,
                    name = p.name,
                    currentStock = p.quantity,
                    unit = p.unit,
                    isLow = p.quantity < (p.minStock ?: 5.0),
                    minStock = p.minStock ?: 5.0,
                    initialStock = p.initialStock ?: 0.0
                )
            }

            val currentYear = year ?: Calendar.getInstance().get(Calendar.YEAR)
            val currentMonth = month ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
            
            val filteredTxs = mockTransactions.filter { tx ->
                try {
                    val parts = tx.date.split("-")
                    val txYear = parts[0].toInt()
                    val txMonth = parts[1].toInt()
                    val matchesDate = if (month == 0) {
                        txYear == currentYear
                    } else {
                        txYear == currentYear && txMonth == currentMonth
                    }
                    matchesDate && (branchId == null || tx.branch?.id == branchId)
                } catch (e: Exception) {
                    false
                }
            }

            val filteredExpenses = mockExpenses.filter { exp ->
                try {
                    val parts = exp.date.split("-")
                    val expYear = parts[0].toInt()
                    val expMonth = parts[1].toInt()
                    val matchesDate = if (month == 0) {
                        expYear == currentYear
                    } else {
                        expYear == currentYear && expMonth == currentMonth
                    }
                    matchesDate && (branchId == null || exp.branch?.id == branchId)
                } catch (e: Exception) {
                    false
                }
            }

            // Sales are only recognized once laundry is claimed / picked up
            val claimedFilteredTxs = filteredTxs.filter { it.pickedUp }
            val totalRevenue = claimedFilteredTxs.sumOf { it.totalAmount }
            val totalExpenses = filteredExpenses.sumOf { it.amount }
            val netProfit = totalRevenue - totalExpenses

            val incomeByDateGroup = claimedFilteredTxs.groupBy { it.date }.map { (date, list) ->
                BreakdownItem(date, list.sumOf { it.totalAmount })
            }.sortedBy { it.name }

            val expenseByCategoryGroup = filteredExpenses.groupBy { it.category }.map { (category, list) ->
                BreakdownItem(category, list.sumOf { it.amount })
            }

            val serviceMap = mutableMapOf<String, Double>()
            claimedFilteredTxs.forEach { tx ->
                tx.serviceItems?.forEach { item ->
                    val name = item.laundryService.name
                    val itemRevenue = item.priceAtTransaction * item.quantity
                    serviceMap[name] = (serviceMap[name] ?: 0.0) + itemRevenue
                }
            }
            val incomeByServiceGroup = serviceMap.map { (name, value) -> BreakdownItem(name, value) }

            val cashRevenue = claimedFilteredTxs.filter { it.paymentMethod.lowercase() == "cash" }.sumOf { it.totalAmount }
            val gcashRevenue = claimedFilteredTxs.filter { it.paymentMethod.lowercase() == "gcash" }.sumOf { it.totalAmount }
            val mopBreakdownList = listOf(
                BreakdownItem("Cash", cashRevenue),
                BreakdownItem("GCash", gcashRevenue)
            )

            val stats = DashboardStats(
                totalTransactionsToday = totalTransactionsToday,
                totalKgWashedToday = totalKgWashedToday,
                totalCustomersToday = totalCustomersToday,
                totalRevenueToday = totalRevenueToday,
                soapStocks = soapStocks,
                totalRevenue = totalRevenue,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                monthlyFinancials = emptyList(),
                expenseCategoryBreakdown = expenseByCategoryGroup,
                incomeServiceBreakdown = incomeByServiceGroup,
                mopBreakdown = mopBreakdownList,
                incomeByDate = incomeByDateGroup,
                expenseByCategory = expenseByCategoryGroup,
                incomeByService = incomeByServiceGroup
            )
            return Result.success(stats)
        }
        return executeCall(RetrofitClient.apiService.getStats(year, month, branchId))
    }

    suspend fun getInventory(): Result<List<SoapProduct>> {
        if (useMock) {
            mockDelay()
            return Result.success(mockProducts)
        }
        return executeCall(RetrofitClient.apiService.getInventory())
    }

    suspend fun createSoapProduct(request: SoapProductRequest): Result<SoapProduct> {
        if (useMock) {
            mockDelay()
            if (mockProducts.any { it.name.equals(request.name, ignoreCase = true) }) {
                return Result.failure(Exception("Product name already exists."))
            }
            val newId = if (mockProducts.isEmpty()) 1 else mockProducts.maxOf { it.id } + 1
            val product = SoapProduct(newId, request.name, request.quantity, request.unit, 5.0, request.quantity)
            mockProducts.add(product)

            val currentUser = getSession()
            val historyId = if (mockHistory.isEmpty()) 1 else mockHistory.maxOf { it.id } + 1
            val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            mockHistory.add(0, SoapInventoryHistory(
                id = historyId,
                createdAt = dateStr,
                soapProduct = SoapProductShort(product.id, product.name, product.unit),
                transactionType = "ADD_STOCK",
                quantityChanged = product.quantity,
                previousQuantity = 0.0,
                newQuantity = product.quantity,
                performedBy = UserShort(currentUser?.id ?: 1, currentUser?.fullName ?: "System"),
                notes = "Initial stock setup"
            ))
            return Result.success(product)
        }
        return executeCall(RetrofitClient.apiService.createSoapProduct(request))
    }

    suspend fun adjustStock(id: Long, request: StockAdjustmentRequest): Result<SoapProduct> {
        if (useMock) {
            mockDelay()
            val index = mockProducts.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Product not found."))
            val p = mockProducts[index]
            val prevQty = p.quantity
            val newQty = prevQty + request.quantityChanged
            if (newQty < 0) {
                return Result.failure(Exception("Insufficient stock! Product has only $prevQty ${p.unit}."))
            }
            p.quantity = newQty
            
            val currentUser = getSession()
            val historyId = if (mockHistory.isEmpty()) 1 else mockHistory.maxOf { it.id } + 1
            val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            mockHistory.add(0, SoapInventoryHistory(
                id = historyId,
                createdAt = dateStr,
                soapProduct = SoapProductShort(p.id, p.name, p.unit),
                transactionType = if (request.quantityChanged > 0) "ADD_STOCK" else "USE_STOCK",
                quantityChanged = request.quantityChanged,
                previousQuantity = prevQty,
                newQuantity = newQty,
                performedBy = UserShort(currentUser?.id ?: 1, currentUser?.fullName ?: "System"),
                notes = request.notes.ifEmpty { "Manual stock adjustment" }
            ))
            return Result.success(p)
        }
        return executeCall(RetrofitClient.apiService.adjustStock(id, request))
    }

    suspend fun deleteSoapProduct(id: Long): Result<Unit> {
        if (useMock) {
            mockDelay()
            val index = mockProducts.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Product not found."))
            
            val hasTransactions = mockTransactions.any { it.soapProduct?.id == id }
            val productHistories = mockHistory.filter { it.soapProduct.id == id }
            
            if (hasTransactions || productHistories.size > 1) {
                return Result.failure(Exception("Cannot delete soap product because it has been used in laundry washes or has manual stock adjustments."))
            }

            mockHistory.removeAll { it.soapProduct.id == id }
            mockProducts.removeAt(index)
            return Result.success(Unit)
        }
        return executeCall(RetrofitClient.apiService.deleteSoapProduct(id)).map { Unit }
    }

    suspend fun getInventoryHistory(): Result<List<SoapInventoryHistory>> {
        if (useMock) {
            mockDelay()
            return Result.success(mockHistory)
        }
        return executeCall(RetrofitClient.apiService.getInventoryHistory())
    }

    suspend fun getTransactions(): Result<List<LaundryTransaction>> {
        if (useMock) {
            mockDelay()
            val currentUser = getSession()
            return if (currentUser?.role == "ROLE_ADMIN") {
                Result.success(mockTransactions.toList())
            } else {
                Result.success(mockTransactions.filter { it.user?.id == currentUser?.id })
            }
        }
        return executeCall(RetrofitClient.apiService.getTransactions())
    }

    suspend fun createTransaction(request: TransactionRequest): Result<LaundryTransaction> {
        if (useMock) {
            mockDelay()
            val category = request.category ?: "laundry"
            var productShort: SoapProductShort? = null
            var remaining = 0.0
            var prevQty = 0.0
            if (category == "laundry" && request.soapProductId != null && request.soapProductId != 0L) {
                val prodIndex = mockProducts.indexOfFirst { it.id == request.soapProductId }
                if (prodIndex == -1) return Result.failure(Exception("Selected soap product not found."))
                val product = mockProducts[prodIndex]
                prevQty = product.quantity
                remaining = prevQty - (request.soapUsedQty ?: 0.0)
                if (remaining < 0) {
                    return Result.failure(Exception("Insufficient stock! ${product.name} has only $prevQty ${product.unit} available."))
                }
                product.quantity = remaining
                productShort = SoapProductShort(product.id, product.name, product.unit)
            }
            
            val currentUser = getSession()
            val dbPerformer = mockUsers.find { it.id == currentUser?.id }
            
            var totalAmount = 0.0
            val serviceItemsList = mutableListOf<TransactionServiceItem>()
            
            request.services.forEach { sReq ->
                val s = mockServices.find { it.id == sReq.serviceId }
                if (s != null) {
                    val price = sReq.priceAtTransaction ?: s.rate
                    totalAmount += price * sReq.quantity
                    serviceItemsList.add(
                        TransactionServiceItem(
                            id = Math.random(),
                            laundryService = s,
                            quantity = sReq.quantity,
                            priceAtTransaction = price
                        )
                    )
                }
            }

            val newTxId = if (mockTransactions.isEmpty()) 1 else mockTransactions.maxOf { it.id } + 1
            val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val tx = LaundryTransaction(
                id = newTxId,
                date = request.date,
                customerName = request.customerName?.trim()?.ifEmpty { null },
                weightKg = request.weightKg,
                soapProduct = productShort,
                soapUsedQty = request.soapUsedQty,
                soapRemainingQty = if (productShort != null) remaining else null,
                user = UserShort(currentUser?.id ?: 1, currentUser?.fullName ?: "System"),
                machineNumber = request.machineNumber,
                branch = dbPerformer?.branch,
                paymentMethod = request.paymentMethod,
                referenceNumber = if (request.paymentMethod.lowercase() == "gcash") request.referenceNumber else null,
                totalAmount = totalAmount,
                createdAt = dateStr,
                serviceItems = serviceItemsList,
                category = category
            )
            mockTransactions.add(0, tx)

            if (productShort != null) {
                val historyId = if (mockHistory.isEmpty()) 1 else mockHistory.maxOf { it.id } + 1
                mockHistory.add(0, SoapInventoryHistory(
                    id = historyId,
                    createdAt = dateStr,
                    soapProduct = productShort,
                    transactionType = "USE_STOCK",
                    quantityChanged = -(request.soapUsedQty ?: 0.0),
                    previousQuantity = prevQty,
                    newQuantity = remaining,
                    performedBy = UserShort(currentUser?.id ?: 1, currentUser?.fullName ?: "System"),
                    notes = "Wash transaction for ${request.customerName ?: "Anonymous"}"
                ))
            }

            return Result.success(tx)
        }
        return executeCall(RetrofitClient.apiService.createTransaction(request))
    }

    suspend fun updateTransaction(id: Long, request: TransactionRequest): Result<LaundryTransaction> {
        if (useMock) {
            mockDelay()
            val txIndex = mockTransactions.indexOfFirst { it.id == id }
            if (txIndex == -1) return Result.failure(Exception("Transaction not found."))
            val oldTx = mockTransactions[txIndex]
            
            // Revert old soap product stock if any
            if (oldTx.soapProduct != null && oldTx.soapUsedQty != null) {
                val prodIndex = mockProducts.indexOfFirst { it.id == oldTx.soapProduct.id }
                if (prodIndex != -1) {
                    mockProducts[prodIndex].quantity += oldTx.soapUsedQty
                    // Log reversion history
                    val historyId = if (mockHistory.isEmpty()) 1 else mockHistory.maxOf { it.id } + 1
                    mockHistory.add(0, SoapInventoryHistory(
                        id = historyId,
                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                        soapProduct = oldTx.soapProduct,
                        transactionType = "ADD_STOCK",
                        quantityChanged = oldTx.soapUsedQty,
                        previousQuantity = mockProducts[prodIndex].quantity - oldTx.soapUsedQty,
                        newQuantity = mockProducts[prodIndex].quantity,
                        performedBy = UserShort(1, "System"),
                        notes = "Reverted transaction ID $id due to edit"
                    ))
                }
            }

            // Apply new stock deduction if any
            val category = request.category ?: "laundry"
            var productShort: SoapProductShort? = null
            var remaining = 0.0
            var prevQty = 0.0
            if (category == "laundry" && request.soapProductId != null && request.soapProductId != 0L) {
                val prodIndex = mockProducts.indexOfFirst { it.id == request.soapProductId }
                if (prodIndex == -1) return Result.failure(Exception("Selected soap product not found."))
                val product = mockProducts[prodIndex]
                prevQty = product.quantity
                remaining = prevQty - (request.soapUsedQty ?: 0.0)
                if (remaining < 0) {
                    return Result.failure(Exception("Insufficient stock! ${product.name} has only $prevQty ${product.unit} available."))
                }
                product.quantity = remaining
                productShort = SoapProductShort(product.id, product.name, product.unit)
            }

            val currentUser = getSession()
            val dbPerformer = mockUsers.find { it.id == currentUser?.id }
            
            var totalAmount = 0.0
            val serviceItemsList = mutableListOf<TransactionServiceItem>()
            request.services.forEach { sReq ->
                val s = mockServices.find { it.id == sReq.serviceId }
                if (s != null) {
                    val price = sReq.priceAtTransaction ?: s.rate
                    totalAmount += price * sReq.quantity
                    serviceItemsList.add(
                        TransactionServiceItem(
                            id = Math.random(),
                            laundryService = s,
                            quantity = sReq.quantity,
                            priceAtTransaction = price
                        )
                    )
                }
            }

            val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val updatedTx = oldTx.copy(
                date = request.date,
                customerName = request.customerName?.trim()?.ifEmpty { null },
                weightKg = request.weightKg,
                soapProduct = productShort,
                soapUsedQty = request.soapUsedQty,
                soapRemainingQty = if (productShort != null) remaining else null,
                machineNumber = request.machineNumber,
                paymentMethod = request.paymentMethod,
                referenceNumber = if (request.paymentMethod.lowercase() == "gcash") request.referenceNumber else null,
                totalAmount = totalAmount,
                serviceItems = serviceItemsList,
                category = category
            )
            mockTransactions[txIndex] = updatedTx

            if (productShort != null) {
                val historyId = if (mockHistory.isEmpty()) 1 else mockHistory.maxOf { it.id } + 1
                mockHistory.add(0, SoapInventoryHistory(
                    id = historyId,
                    createdAt = dateStr,
                    soapProduct = productShort,
                    transactionType = "USE_STOCK",
                    quantityChanged = -(request.soapUsedQty ?: 0.0),
                    previousQuantity = prevQty,
                    newQuantity = remaining,
                    performedBy = UserShort(currentUser?.id ?: 1, currentUser?.fullName ?: "System"),
                    notes = "Edited transaction ID $id"
                ))
            }

            return Result.success(updatedTx)
        }
        return executeCall(RetrofitClient.apiService.updateTransaction(id, request))
    }

    suspend fun markTransactionAsPickedUp(id: Long): Result<LaundryTransaction> {
        if (useMock) {
            mockDelay()
            val txIndex = mockTransactions.indexOfFirst { it.id == id }
            if (txIndex == -1) return Result.failure(Exception("Transaction not found."))
            val oldTx = mockTransactions[txIndex]
            
            val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val updatedTx = oldTx.copy(
                pickedUp = true,
                pickedUpAt = dateStr
            )
            mockTransactions[txIndex] = updatedTx
            return Result.success(updatedTx)
        }
        return executeCall(RetrofitClient.apiService.markTransactionAsPickedUp(id))
    }

    suspend fun getUsers(): Result<List<User>> {
        if (useMock) {
            mockDelay()
            return Result.success(mockUsers)
        }
        return executeCall(RetrofitClient.apiService.getUsers())
    }

    suspend fun createUser(request: UserRequest): Result<User> {
        if (useMock) {
            mockDelay()
            if (mockUsers.any { it.username.equals(request.username, ignoreCase = true) }) {
                return Result.failure(Exception("Username already exists."))
            }
            val branchObj = request.branchId?.let { bId -> mockBranches.find { it.id == bId } }
            val newId = if (mockUsers.isEmpty()) 1 else mockUsers.maxOf { it.id } + 1
            val user = User(newId, request.username, request.fullName, request.role, request.active, branchObj)
            mockUsers.add(user)
            if (request.password != null) {
                mockUserPasswords[request.username] = request.password
            }
            return Result.success(user)
        }
        return executeCall(RetrofitClient.apiService.createUser(request))
    }

    suspend fun updateUser(id: Long, request: UserRequest): Result<User> {
        if (useMock) {
            mockDelay()
            val index = mockUsers.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("User not found."))
            if (mockUsers.any { it.id != id && it.username.equals(request.username, ignoreCase = true) }) {
                return Result.failure(Exception("Username is already taken."))
            }
            val user = mockUsers[index]
            val branchObj = request.branchId?.let { bId -> mockBranches.find { it.id == bId } }
            
            val updatedUser = user.copy(
                username = request.username,
                fullName = request.fullName,
                role = request.role,
                active = request.active,
                branch = branchObj
            )
            mockUsers[index] = updatedUser
            if (!request.password.isNullOrEmpty()) {
                mockUserPasswords[request.username] = request.password
            }
            return Result.success(updatedUser)
        }
        return executeCall(RetrofitClient.apiService.updateUser(id, request))
    }

    suspend fun toggleUserActive(id: Long): Result<User> {
        if (useMock) {
            mockDelay()
            val index = mockUsers.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("User not found."))
            val u = mockUsers[index]
            val updated = u.copy(active = !u.active)
            mockUsers[index] = updated
            return Result.success(updated)
        }
        return executeCall(RetrofitClient.apiService.toggleUserActive(id))
    }

    suspend fun deleteUser(id: Long): Result<Unit> {
        if (useMock) {
            mockDelay()
            val index = mockUsers.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("User not found."))
            val user = mockUsers.removeAt(index)
            mockUserPasswords.remove(user.username)
            return Result.success(Unit)
        }
        return executeCall(RetrofitClient.apiService.deleteUser(id)).map { Unit }
    }

    suspend fun getExpenses(): Result<List<Expense>> {
        if (useMock) {
            mockDelay()
            return Result.success(mockExpenses)
        }
        return executeCall(RetrofitClient.apiService.getExpenses())
    }

    suspend fun createExpense(request: ExpenseRequest): Result<Expense> {
        if (useMock) {
            mockDelay()
            val branchObj = request.branch?.id?.let { bId -> mockBranches.find { it.id == bId } }
            val newId = if (mockExpenses.isEmpty()) 1 else mockExpenses.maxOf { it.id } + 1
            val expense = Expense(newId, request.category, request.amount, request.date, request.description, branchObj)
            mockExpenses.add(expense)
            return Result.success(expense)
        }
        return executeCall(RetrofitClient.apiService.createExpense(request))
    }

    suspend fun updateExpense(id: Long, request: ExpenseRequest): Result<Expense> {
        if (useMock) {
            mockDelay()
            val index = mockExpenses.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Expense not found."))
            val branchObj = request.branch?.id?.let { bId -> mockBranches.find { it.id == bId } }
            val expense = Expense(id, request.category, request.amount, request.date, request.description, branchObj)
            mockExpenses[index] = expense
            return Result.success(expense)
        }
        return executeCall(RetrofitClient.apiService.updateExpense(id, request))
    }

    suspend fun deleteExpense(id: Long): Result<Unit> {
        if (useMock) {
            mockDelay()
            val index = mockExpenses.indexOfFirst { it.id == id }
            if (index == -1) return Result.failure(Exception("Expense not found."))
            mockExpenses.removeAt(index)
            return Result.success(Unit)
        }
        return executeCall(RetrofitClient.apiService.deleteExpense(id)).map { Unit }
    }

    suspend fun getReports(type: String, branchId: Long?): Result<List<ReportItem>> {
        if (useMock) {
            mockDelay()
            val groupings = mutableMapOf<String, ReportItem>()
            
            val filteredTxs = if (branchId != null) {
                mockTransactions.filter { it.branch?.id == branchId }
            } else {
                mockTransactions
            }

            filteredTxs.forEach { t ->
                val period = when (type.lowercase()) {
                    "daily" -> t.date
                    "weekly" -> {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val d = sdf.parse(t.date)
                            val cal = Calendar.getInstance()
                            if (d != null) cal.time = d
                            val day = cal.get(Calendar.DAY_OF_WEEK)
                            cal.add(Calendar.DATE, -day + if (day == Calendar.SUNDAY) -6 else 2)
                            "Week of " + sdf.format(cal.time)
                        } catch (e: Exception) {
                            "Unknown Week"
                        }
                    }
                    else -> {
                        try {
                            val parts = t.date.split("-")
                            val year = parts[0]
                            val monthNum = parts[1].toInt()
                            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            "${monthNames[monthNum - 1]} $year"
                        } catch (e: Exception) {
                            "Unknown Month"
                        }
                    }
                }

                val current = groupings[period] ?: ReportItem(
                    period = period,
                    transactionCount = 0,
                    totalKgWashed = 0.0,
                    totalSoapUsed = 0.0,
                    machineUsage = mutableMapOf(),
                    branchUsage = mutableMapOf(),
                    totalRevenue = 0.0,
                    customerCount = 0
                )

                val mUsage = (current.machineUsage ?: emptyMap()).toMutableMap()
                t.machineNumber?.let { mNum ->
                    mUsage[mNum] = (mUsage[mNum] ?: 0L) + 1L
                }
                
                val bUsage = (current.branchUsage ?: emptyMap()).toMutableMap()
                t.branch?.name?.let { bName ->
                    bUsage[bName] = (bUsage[bName] ?: 0L) + 1L
                }

                val cNames = filteredTxs.filter { it.date == t.date }.mapNotNull { it.customerName?.trim() }.filter { it.isNotEmpty() }.distinct()
                val customerCount = if (type.lowercase() == "daily") cNames.size.toLong().let { if (it == 0L) current.transactionCount + 1 else it } else current.transactionCount + 1

                groupings[period] = current.copy(
                    transactionCount = current.transactionCount + 1,
                    totalKgWashed = current.totalKgWashed + (t.weightKg ?: 0.0),
                    totalSoapUsed = current.totalSoapUsed + (t.soapUsedQty ?: 0.0),
                    machineUsage = mUsage,
                    branchUsage = bUsage,
                    totalRevenue = current.totalRevenue + if (t.pickedUp) t.totalAmount else 0.0,
                    customerCount = customerCount
                )
            }

            val list = groupings.values.toList().sortedByDescending { it.period }
            return Result.success(list)
        }

        return when (type.lowercase()) {
            "daily" -> executeCall(RetrofitClient.apiService.getDailyReports(branchId))
            "weekly" -> executeCall(RetrofitClient.apiService.getWeeklyReports(branchId))
            else -> executeCall(RetrofitClient.apiService.getMonthlyReports(branchId))
        }
    }
}
