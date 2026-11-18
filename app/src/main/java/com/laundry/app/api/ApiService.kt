package com.laundry.app.api

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("/api/auth/login")
    fun login(@Body request: LoginRequest): Call<JwtResponse>

    // Branches
    @GET("/api/branches")
    fun getBranches(): Call<List<Branch>>

    @POST("/api/branches")
    fun createBranch(@Body request: BranchRequest): Call<Branch>

    @DELETE("/api/branches/{id}")
    fun deleteBranch(@Path("id") id: Long): Call<Void>

    @PUT("/api/branches/{id}")
    fun updateBranch(@Path("id") id: Long, @Body request: BranchRequest): Call<Branch>

    // Services
    @GET("/api/services")
    fun getServices(): Call<List<LaundryService>>

    @POST("/api/services")
    fun createService(@Body request: LaundryServiceRequest): Call<LaundryService>

    @PUT("/api/services/{id}")
    fun updateService(@Path("id") id: Long, @Body request: LaundryServiceRequest): Call<LaundryService>

    @DELETE("/api/services/{id}")
    fun deleteService(@Path("id") id: Long): Call<Void>

    // Dashboard
    @GET("/api/dashboard/stats")
    fun getStats(
        @Query("year") year: Int?,
        @Query("month") month: Int?,
        @Query("branchId") branchId: Long?
    ): Call<DashboardStats>

    // Inventory
    @GET("/api/inventory")
    fun getInventory(): Call<List<SoapProduct>>

    @POST("/api/inventory")
    fun createSoapProduct(@Body request: SoapProductRequest): Call<SoapProduct>

    @DELETE("/api/inventory/{id}")
    fun deleteSoapProduct(@Path("id") id: Long): Call<Void>

    @PUT("/api/inventory/{id}/adjust")
    fun adjustStock(
        @Path("id") id: Long,
        @Body request: StockAdjustmentRequest
    ): Call<SoapProduct>

    @GET("/api/inventory/history")
    fun getInventoryHistory(): Call<List<SoapInventoryHistory>>

    // Transactions
    @GET("/api/transactions")
    fun getTransactions(): Call<List<LaundryTransaction>>

    @POST("/api/transactions")
    fun createTransaction(@Body request: TransactionRequest): Call<LaundryTransaction>

    @PUT("/api/transactions/{id}")
    fun updateTransaction(@Path("id") id: Long, @Body request: TransactionRequest): Call<LaundryTransaction>

    @PUT("/api/transactions/{id}/pickup")
    fun markTransactionAsPickedUp(@Path("id") id: Long): Call<LaundryTransaction>

    // Users
    @GET("/api/users")
    fun getUsers(): Call<List<User>>

    @POST("/api/users")
    fun createUser(@Body request: UserRequest): Call<User>

    @PUT("/api/users/{id}")
    fun updateUser(@Path("id") id: Long, @Body request: UserRequest): Call<User>

    @PUT("/api/users/{id}/toggle")
    fun toggleUserActive(@Path("id") id: Long): Call<User>

    @DELETE("/api/users/{id}")
    fun deleteUser(@Path("id") id: Long): Call<Void>

    // Expenses
    @GET("/api/expenses")
    fun getExpenses(): Call<List<Expense>>

    @POST("/api/expenses")
    fun createExpense(@Body request: ExpenseRequest): Call<Expense>

    @PUT("/api/expenses/{id}")
    fun updateExpense(@Path("id") id: Long, @Body request: ExpenseRequest): Call<Expense>

    @DELETE("/api/expenses/{id}")
    fun deleteExpense(@Path("id") id: Long): Call<Void>

    // Reports
    @GET("/api/reports/daily")
    fun getDailyReports(@Query("branchId") branchId: Long?): Call<List<ReportItem>>

    @GET("/api/reports/weekly")
    fun getWeeklyReports(@Query("branchId") branchId: Long?): Call<List<ReportItem>>

    @GET("/api/reports/monthly")
    fun getMonthlyReports(@Query("branchId") branchId: Long?): Call<List<ReportItem>>
}
