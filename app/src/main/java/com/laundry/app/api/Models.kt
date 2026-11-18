package com.laundry.app.api

data class LoginRequest(
    val username: String,
    val password: String
)

data class JwtResponse(
    val token: String,
    val id: Long,
    val username: String,
    val fullName: String,
    val role: String,
    val branchName: String?,
    val branchId: Long?
)

data class Branch(
    val id: Long,
    val name: String,
    val location: String,
    val machinesConfig: String? = "Machine 1:true,Machine 2:true,Machine 3:true,Machine 4:true",
    val createdAt: String? = null
)

data class BranchRequest(
    val name: String,
    val location: String,
    val machinesConfig: String? = "Machine 1:true,Machine 2:true,Machine 3:true,Machine 4:true"
)

data class User(
    val id: Long,
    val username: String,
    val fullName: String,
    val role: String,
    val active: Boolean,
    val branch: Branch?
)

data class UserRequest(
    val username: String,
    val password: String? = null,
    val fullName: String,
    val role: String,
    val active: Boolean = true,
    val branchId: Long? = null
)

data class LaundryService(
    val id: Long,
    val name: String,
    val rate: Double,
    val unit: String
)

data class LaundryServiceRequest(
    val name: String,
    val rate: Double,
    val unit: String
)

data class SoapProduct(
    val id: Long,
    val name: String,
    var quantity: Double,
    val unit: String,
    val minStock: Double? = 5.0,
    val initialStock: Double? = 0.0
)

data class SoapProductRequest(
    val name: String,
    val quantity: Double,
    val unit: String
)

data class StockAdjustmentRequest(
    val quantityChanged: Double,
    val notes: String
)

data class SoapInventoryHistory(
    val id: Long,
    val createdAt: String,
    val soapProduct: SoapProductShort,
    val transactionType: String,
    val quantityChanged: Double,
    val previousQuantity: Double,
    val newQuantity: Double,
    val performedBy: UserShort,
    val notes: String?
)

data class SoapProductShort(
    val id: Long,
    val name: String,
    val unit: String
)

data class UserShort(
    val id: Long,
    val fullName: String
)

data class TransactionServiceItem(
    val id: Double,
    val laundryService: LaundryService,
    val quantity: Int,
    val priceAtTransaction: Double
)

data class LaundryTransaction(
    val id: Long,
    val date: String,
    val customerName: String?,
    val weightKg: Double?,
    val soapProduct: SoapProductShort?,
    val soapUsedQty: Double?,
    val soapRemainingQty: Double?,
    val user: UserShort?,
    val machineNumber: String?,
    val branch: Branch?,
    val paymentMethod: String,
    val referenceNumber: String?,
    val totalAmount: Double,
    val createdAt: String? = null,
    val serviceItems: List<TransactionServiceItem>?,
    val pickedUp: Boolean = false,
    val pickedUpAt: String? = null,
    val category: String? = "laundry"
)

data class ServiceItemRequest(
    val serviceId: Long,
    val quantity: Int,
    val priceAtTransaction: Double? = null
)

data class TransactionRequest(
    val date: String,
    val customerName: String?,
    val weightKg: Double?,
    val soapProductId: Long?,
    val soapUsedQty: Double?,
    val machineNumber: String?,
    val paymentMethod: String,
    val referenceNumber: String?,
    val services: List<ServiceItemRequest>,
    val category: String? = "laundry"
)

data class Expense(
    val id: Long,
    val category: String,
    val amount: Double,
    val date: String,
    val description: String?,
    val branch: Branch?
)

data class ExpenseRequest(
    val category: String,
    val amount: Double,
    val date: String,
    val description: String?,
    val branch: BranchIdHolder?
)

data class BranchIdHolder(
    val id: Long
)

data class DashboardStats(
    val totalTransactionsToday: Long,
    val totalKgWashedToday: Double,
    val totalCustomersToday: Long,
    val totalRevenueToday: Double = 0.0,
    val soapStocks: List<SoapStockStatus>?,
    val totalRevenue: Double? = 0.0,
    val totalExpenses: Double? = 0.0,
    val netProfit: Double? = 0.0,
    val monthlyFinancials: List<MonthlyFinancialPoint>? = emptyList(),
    val expenseCategoryBreakdown: List<BreakdownItem>? = emptyList(),
    val incomeServiceBreakdown: List<BreakdownItem>? = emptyList(),
    val mopBreakdown: List<BreakdownItem>? = emptyList(),
    val incomeByDate: List<BreakdownItem>? = emptyList(),
    val expenseByCategory: List<BreakdownItem>? = emptyList(),
    val incomeByService: List<BreakdownItem>? = emptyList()
)

data class SoapStockStatus(
    val id: Long,
    val name: String,
    val currentStock: Double,
    val unit: String,
    val isLow: Boolean,
    val minStock: Double = 5.0,
    val initialStock: Double = 0.0
)

data class MonthlyFinancialPoint(
    val month: String,
    val revenue: Double,
    val expenses: Double
)

data class BreakdownItem(
    val name: String,
    val value: Double
)

data class ReportItem(
    val period: String,
    val transactionCount: Long,
    val totalKgWashed: Double,
    val totalSoapUsed: Double,
    val machineUsage: Map<String, Long>?,
    val branchUsage: Map<String, Long>?,
    val totalRevenue: Double,
    val customerCount: Long
)
