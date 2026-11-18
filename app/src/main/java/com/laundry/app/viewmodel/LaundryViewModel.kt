package com.laundry.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laundry.app.api.*
import com.laundry.app.repository.LaundryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LaundryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LaundryRepository(application.applicationContext)

    val useMock = MutableStateFlow(repository.useMock)
    val apiBaseUrl = MutableStateFlow(RetrofitClient.getBaseUrl())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<JwtResponse?>(null)
    val currentUser: StateFlow<JwtResponse?> = _currentUser.asStateFlow()

    private val _branches = MutableStateFlow<List<Branch>>(emptyList())
    val branches: StateFlow<List<Branch>> = _branches.asStateFlow()

    private val _services = MutableStateFlow<List<LaundryService>>(emptyList())
    val services: StateFlow<List<LaundryService>> = _services.asStateFlow()

    private val _products = MutableStateFlow<List<SoapProduct>>(emptyList())
    val products: StateFlow<List<SoapProduct>> = _products.asStateFlow()

    private val _transactions = MutableStateFlow<List<LaundryTransaction>>(emptyList())
    val transactions: StateFlow<List<LaundryTransaction>> = _transactions.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _inventoryHistory = MutableStateFlow<List<SoapInventoryHistory>>(emptyList())
    val inventoryHistory: StateFlow<List<SoapInventoryHistory>> = _inventoryHistory.asStateFlow()

    private val _dashboardStats = MutableStateFlow<DashboardStats?>(null)
    val dashboardStats: StateFlow<DashboardStats?> = _dashboardStats.asStateFlow()

    private val _reports = MutableStateFlow<List<ReportItem>>(emptyList())
    val reports: StateFlow<List<ReportItem>> = _reports.asStateFlow()

    init {
        _currentUser.value = repository.getSession()
    }

    fun toggleUseMock(enabled: Boolean) {
        repository.useMock = enabled
        useMock.value = enabled
        _dashboardStats.value = null
        _branches.value = emptyList()
        _services.value = emptyList()
        _products.value = emptyList()
        _transactions.value = emptyList()
        _expenses.value = emptyList()
        _users.value = emptyList()
        _inventoryHistory.value = emptyList()
        _reports.value = emptyList()
    }

    fun setApiBaseUrl(url: String) {
        repository.baseUrl = url
        RetrofitClient.setBaseUrl(url)
        apiBaseUrl.value = RetrofitClient.getBaseUrl()
    }

    fun clearFeedback() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun login(request: LoginRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            
            if (request.username.isBlank() || request.password.isBlank()) {
                _errorMessage.value = "Username and password are required."
                _isLoading.value = false
                onResult(false)
                return@launch
            }

            repository.login(request)
                .onSuccess { response ->
                    repository.saveSession(response)
                    _currentUser.value = response
                    _successMessage.value = "Welcome back, ${response.fullName}!"
                    onResult(true)
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Authentication failed."
                    onResult(false)
                }
            _isLoading.value = false
        }
    }

    fun logout() {
        repository.clearSession()
        _currentUser.value = null
        clearFeedback()
    }

    fun fetchBranches() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getBranches()
                .onSuccess { _branches.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun createBranch(name: String, location: String, machinesConfig: String? = null) {
        viewModelScope.launch {
            clearFeedback()
            if (name.isBlank() || location.isBlank()) {
                _errorMessage.value = "Branch name and address location are required."
                return@launch
            }
            _isLoading.value = true
            repository.createBranch(BranchRequest(name.trim(), location.trim(), machinesConfig))
                .onSuccess {
                    _successMessage.value = "Branch '${it.name}' established successfully."
                    fetchBranches()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateBranch(id: Long, name: String, location: String, machinesConfig: String?) {
        viewModelScope.launch {
            clearFeedback()
            if (name.isBlank() || location.isBlank()) {
                _errorMessage.value = "Branch name and address location are required."
                return@launch
            }
            _isLoading.value = true
            repository.updateBranch(id, BranchRequest(name.trim(), location.trim(), machinesConfig))
                .onSuccess {
                    _successMessage.value = "Branch '${it.name}' updated successfully."
                    fetchBranches()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteBranch(id: Long) {
        viewModelScope.launch {
            clearFeedback()
            _isLoading.value = true
            repository.deleteBranch(id)
                .onSuccess {
                    _successMessage.value = "Branch removed successfully."
                    fetchBranches()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchServices() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getServices()
                .onSuccess { _services.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun createService(name: String, rate: Double, unit: String) {
        viewModelScope.launch {
            clearFeedback()
            if (name.isBlank() || rate < 0 || unit.isBlank()) {
                _errorMessage.value = "Valid Name, non-negative Rate, and Unit are required."
                return@launch
            }
            _isLoading.value = true
            repository.createService(LaundryServiceRequest(name.trim(), rate, unit.trim()))
                .onSuccess {
                    _successMessage.value = "Service '${it.name}' added."
                    fetchServices()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateService(id: Long, name: String, rate: Double, unit: String) {
        viewModelScope.launch {
            clearFeedback()
            if (name.isBlank() || rate < 0 || unit.isBlank()) {
                _errorMessage.value = "Valid Name, non-negative Rate, and Unit are required."
                return@launch
            }
            _isLoading.value = true
            repository.updateService(id, LaundryServiceRequest(name.trim(), rate, unit.trim()))
                .onSuccess {
                    _successMessage.value = "Service updated successfully."
                    fetchServices()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteService(id: Long) {
        viewModelScope.launch {
            clearFeedback()
            _isLoading.value = true
            repository.deleteService(id)
                .onSuccess {
                    _successMessage.value = "Service deleted."
                    fetchServices()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchDashboardStats(year: Int?, month: Int?, branchId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getStats(year, month, branchId)
                .onSuccess { _dashboardStats.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getInventory()
                .onSuccess { _products.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun createSoapProduct(name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            clearFeedback()
            if (name.isBlank() || quantity < 0 || unit.isBlank()) {
                _errorMessage.value = "Please enter valid item name, positive quantity, and unit metric."
                return@launch
            }
            _isLoading.value = true
            repository.createSoapProduct(SoapProductRequest(name.trim(), quantity, unit.trim()))
                .onSuccess {
                    _successMessage.value = "Product resource '${it.name}' registered."
                    fetchInventory()
                    fetchDashboardStats(null, null)
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun adjustStock(id: Long, quantityChanged: Double, notes: String) {
        viewModelScope.launch {
            clearFeedback()
            if (quantityChanged == 0.0) {
                _errorMessage.value = "Adjustment amount cannot be zero."
                return@launch
            }
            _isLoading.value = true
            repository.adjustStock(id, StockAdjustmentRequest(quantityChanged, notes.trim()))
                .onSuccess {
                    _successMessage.value = "Stock adjustment logged successfully."
                    fetchInventory()
                    fetchInventoryHistory()
                    fetchDashboardStats(null, null)
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteSoapProduct(id: Long) {
        viewModelScope.launch {
            clearFeedback()
            _isLoading.value = true
            repository.deleteSoapProduct(id)
                .onSuccess {
                    _successMessage.value = "Inventory item removed."
                    fetchInventory()
                    fetchDashboardStats(null, null)
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchInventoryHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getInventoryHistory()
                .onSuccess { _inventoryHistory.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getTransactions()
                .onSuccess { _transactions.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun markTransactionAsPickedUp(id: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.markTransactionAsPickedUp(id)
                .onSuccess {
                    _successMessage.value = "Laundry successfully marked as claimed! Amount added to sales."
                    fetchTransactions()
                    fetchDashboardStats(null, null)
                    onResult(true)
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Failed to mark laundry as claimed."
                    onResult(false)
                }
            _isLoading.value = false
        }
    }

    fun createTransaction(request: TransactionRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()

            val isLaundry = (request.category ?: "laundry").equals("laundry", ignoreCase = true)
            if (isLaundry) {
                if ((request.weightKg ?: 0.0) <= 0.0) {
                    _errorMessage.value = "Weight must be greater than 0 kg."
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
                if ((request.soapUsedQty ?: 0.0) < 0.0) {
                    _errorMessage.value = "Soap used quantity cannot be negative."
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
            }
            if (request.services.isEmpty()) {
                _errorMessage.value = "Please select at least one laundry service."
                _isLoading.value = false
                onResult(false)
                return@launch
            }
            if (request.paymentMethod.lowercase() == "gcash") {
                val ref = request.referenceNumber?.trim()
                if (ref.isNullOrEmpty() || ref.length != 4 || ref.toIntOrNull() == null) {
                    _errorMessage.value = "Please enter exactly the last 4 digits of the GCash reference number."
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
            }

            repository.createTransaction(request)
                .onSuccess {
                    _successMessage.value = "Transaction successfully recorded!"
                    fetchTransactions()
                    fetchInventory()
                    onResult(true)
                }
                .onFailure {
                    _errorMessage.value = it.message
                    onResult(false)
                }
            _isLoading.value = false
        }
    }

    fun updateTransaction(id: Long, request: TransactionRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()

            val isLaundry = (request.category ?: "laundry").equals("laundry", ignoreCase = true)
            if (isLaundry) {
                if ((request.weightKg ?: 0.0) <= 0.0) {
                    _errorMessage.value = "Weight must be greater than 0 kg."
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
                if ((request.soapUsedQty ?: 0.0) < 0.0) {
                    _errorMessage.value = "Soap used quantity cannot be negative."
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
            }
            if (request.services.isEmpty()) {
                _errorMessage.value = "Please select at least one service."
                _isLoading.value = false
                onResult(false)
                return@launch
            }
            if (request.paymentMethod.lowercase() == "gcash") {
                val ref = request.referenceNumber?.trim()
                if (ref.isNullOrEmpty() || ref.length != 4 || ref.toIntOrNull() == null) {
                    _errorMessage.value = "Please enter exactly the last 4 digits of the GCash reference number."
                    _isLoading.value = false
                    onResult(false)
                    return@launch
                }
            }

            repository.updateTransaction(id, request)
                .onSuccess {
                    _successMessage.value = "Transaction successfully updated!"
                    fetchTransactions()
                    fetchInventory()
                    onResult(true)
                }
                .onFailure {
                    _errorMessage.value = it.message
                    onResult(false)
                }
            _isLoading.value = false
        }
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getUsers()
                .onSuccess { _users.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun createUser(request: UserRequest) {
        viewModelScope.launch {
            clearFeedback()
            if (request.username.isBlank() || request.fullName.isBlank() || request.password.isNullOrBlank()) {
                _errorMessage.value = "Username, password, and full name are required."
                return@launch
            }
            _isLoading.value = true
            repository.createUser(request)
                .onSuccess {
                    _successMessage.value = "User account created successfully."
                    fetchUsers()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateUser(id: Long, request: UserRequest) {
        viewModelScope.launch {
            clearFeedback()
            if (request.username.isBlank() || request.fullName.isBlank()) {
                _errorMessage.value = "Username and full name are required."
                return@launch
            }
            _isLoading.value = true
            repository.updateUser(id, request)
                .onSuccess {
                    _successMessage.value = "User settings updated."
                    fetchUsers()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun toggleUserActive(id: Long) {
        viewModelScope.launch {
            clearFeedback()
            _isLoading.value = true
            repository.toggleUserActive(id)
                .onSuccess {
                    _successMessage.value = "Operator account status toggled."
                    fetchUsers()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            clearFeedback()
            _isLoading.value = true
            repository.deleteUser(id)
                .onSuccess {
                    _successMessage.value = "Operator account deleted."
                    fetchUsers()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchExpenses() {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getExpenses()
                .onSuccess { _expenses.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun createExpense(category: String, amount: Double, date: String, description: String, branchId: Long?) {
        viewModelScope.launch {
            clearFeedback()
            if (category.isBlank() || amount < 0 || date.isBlank()) {
                _errorMessage.value = "Please select category, input valid amount, and specify date."
                return@launch
            }
            _isLoading.value = true
            val branchHolder = branchId?.let { BranchIdHolder(it) }
            repository.createExpense(ExpenseRequest(category, amount, date, description.trim(), branchHolder))
                .onSuccess {
                    _successMessage.value = "Expense logged successfully."
                    fetchExpenses()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateExpense(id: Long, category: String, amount: Double, date: String, description: String, branchId: Long?) {
        viewModelScope.launch {
            clearFeedback()
            if (category.isBlank() || amount < 0 || date.isBlank()) {
                _errorMessage.value = "Please select category, input valid amount, and specify date."
                return@launch
            }
            _isLoading.value = true
            val branchHolder = branchId?.let { BranchIdHolder(it) }
            repository.updateExpense(id, ExpenseRequest(category, amount, date, description.trim(), branchHolder))
                .onSuccess {
                    _successMessage.value = "Expense record updated."
                    fetchExpenses()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            clearFeedback()
            _isLoading.value = true
            repository.deleteExpense(id)
                .onSuccess {
                    _successMessage.value = "Expense ledger entry deleted."
                    fetchExpenses()
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun fetchReports(type: String, branchId: Long?) {
        viewModelScope.launch {
            _isLoading.value = true
            clearFeedback()
            repository.getReports(type, branchId)
                .onSuccess { _reports.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }
}
