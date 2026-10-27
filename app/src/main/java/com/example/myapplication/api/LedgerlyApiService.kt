package com.example.myapplication.api

import android.content.Context
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.model.AuthResponse
import com.example.myapplication.model.Budget
import com.example.myapplication.model.BudgetRequest
import com.example.myapplication.model.DashboardData
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.RegisterRequest
import com.example.myapplication.model.ReportsData
import com.example.myapplication.model.Salary
import com.example.myapplication.model.SalaryRequest
import com.example.myapplication.model.Transaction
import com.example.myapplication.model.TransactionRequest
import com.example.myapplication.model.UpdatePasswordRequest
import com.example.myapplication.model.UpdateProfileRequest
import com.example.myapplication.model.User
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Retrofit API interface for Ledgerly backend
interface LedgerlyApiService {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun getMe(): User

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body req: UpdateProfileRequest): User

    @PUT("api/auth/password")
    suspend fun updatePassword(@Body req: UpdatePasswordRequest): Map<String, String>

    // Transactions
    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<Transaction>

    @POST("api/transactions")
    suspend fun createTransaction(@Body req: TransactionRequest): Transaction

    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body req: TransactionRequest): Transaction

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>

    // Salary
    @GET("api/salary")
    suspend fun getSalaries(): List<Salary>

    @POST("api/salary")
    suspend fun createSalary(@Body req: SalaryRequest): Salary

    @PUT("api/salary/{id}")
    suspend fun updateSalary(@Path("id") id: String, @Body req: SalaryRequest): Salary

    @DELETE("api/salary/{id}")
    suspend fun deleteSalary(@Path("id") id: String): Response<Unit>

    // Budgets
    @GET("api/budgets")
    suspend fun getBudgets(@Query("month") month: String? = null): List<Budget>

    @POST("api/budgets")
    suspend fun upsertBudget(@Body req: BudgetRequest): Budget

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: String): Response<Unit>

    // Dashboard & Reports
    @GET("api/dashboard")
    suspend fun getDashboard(): DashboardData

    @GET("api/reports")
    suspend fun getReports(@Query("range") range: String = "30d"): ReportsData
}

// Retrofit instance with bearer token authorization and Windows firewall fallback
object RetrofitInstance {
    private const val PRIMARY_BASE_URL = "http://127.0.0.1:3000/"

    private var tokenStore: TokenStore? = null
    private var onUnauthorizedCallback: (() -> Unit)? = null

    fun init(context: Context, onUnauthorized: () -> Unit = {}) {
        tokenStore = TokenStore(context)
        onUnauthorizedCallback = onUnauthorized
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                tokenStore?.token?.let { token ->
                    if (token.isNotBlank()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                }

                val request = requestBuilder.build()
                val response: okhttp3.Response = chain.proceed(request)

                if (response.code() == 401) {
                    tokenStore?.clear()
                    onUnauthorizedCallback?.invoke()
                }

                response
            }
            .build()
    }

    val api: LedgerlyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PRIMARY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LedgerlyApiService::class.java)
    }
}
