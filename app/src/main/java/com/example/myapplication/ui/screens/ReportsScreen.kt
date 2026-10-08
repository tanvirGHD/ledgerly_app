package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.BarChart
import com.example.myapplication.ui.components.DonutChart
import com.example.myapplication.ui.components.ShimmerCardPlaceholder
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.LedgerlyViewModel

@Composable
fun ReportsScreen(
    authViewModel: AuthViewModel,
    viewModel: LedgerlyViewModel
) {
    val context = LocalContext.current
    val reportsData by viewModel.reports.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val selectedRange by viewModel.reportRange.collectAsState()

    val ranges = listOf("7d", "4w", "6m", "year")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports & Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(onClick = { viewModel.exportTransactionsCsv(context) }) {
                Text("Export CSV")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Range Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ranges.forEach { range ->
                FilterChip(
                    selected = (selectedRange == range),
                    onClick = {
                        viewModel.reportRange.value = range
                        viewModel.fetchReports()
                    },
                    label = { Text(range.uppercase()) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isInitialLoading && reportsData == null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ShimmerCardPlaceholder(heightDp = 180)
                ShimmerCardPlaceholder(heightDp = 180)
            }
        } else {
            val data = reportsData ?: com.example.myapplication.model.ReportsData()

            // 1. Income vs Expense Bar Chart
            Text(
                text = "Income vs Expense ($selectedRange)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                BarChart(
                    data = data.series,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Expenses by Category Pie / Donut Chart
            Text(
                text = "Expenses by Category ($selectedRange)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                DonutChart(
                    data = data.byCategory,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}



