package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Salary
import com.example.myapplication.ui.components.ShimmerCardPlaceholder
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.LedgerlyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(
    authViewModel: AuthViewModel,
    viewModel: LedgerlyViewModel
) {
    val currency by authViewModel.currency.collectAsState()
    val salaries by viewModel.salaries.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val symbol = if (currency == "BDT") "৳" else "$"

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingSalary by remember { mutableStateOf<Salary?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSalary = null
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Salary")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadDataInParallel() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Salary & Pay Stubs",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isInitialLoading && salaries.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShimmerCardPlaceholder(heightDp = 80)
                        ShimmerCardPlaceholder(heightDp = 80)
                    }
                } else if (salaries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No salary records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = salaries,
                            key = { it.id ?: it.hashCode() }
                        ) { sal ->
                            SwipeToDismissSalaryItem(
                                sal = sal,
                                symbol = symbol,
                                onEdit = {
                                    editingSalary = sal
                                    showBottomSheet = true
                                },
                                onDelete = { viewModel.deleteSalary(sal) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SalaryFormBottomSheet(
                salary = editingSalary,
                viewModel = viewModel,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissSalaryItem(
    sal: Salary,
    symbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEF4444), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        val netAmount = sal.amount + sal.bonus - sal.deduction

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onEdit() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sal.source, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Month: ${sal.month}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Base: $symbol${sal.amount} | Bonus: +$symbol${sal.bonus} | Deduct: -$symbol${sal.deduction}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "$symbol${String.format("%.2f", netAmount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}

@Composable
fun SalaryFormBottomSheet(
    salary: Salary?,
    viewModel: LedgerlyViewModel,
    onDismiss: () -> Unit
) {
    val isEdit = salary != null

    var source by remember { mutableStateOf(salary?.source ?: "") }
    var amountText by remember { mutableStateOf(salary?.amount?.toString() ?: "") }
    var bonusText by remember { mutableStateOf(salary?.bonus?.toString() ?: "0") }
    var deductionText by remember { mutableStateOf(salary?.deduction?.toString() ?: "0") }
    var month by remember { mutableStateOf(salary?.month ?: viewModel.getCurrentMonth()) }
    var note by remember { mutableStateOf(salary?.note ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isEdit) "Edit Salary" else "Add Salary",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = source,
            onValueChange = { source = it },
            label = { Text("Company / Source *") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Base Amount *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bonusText,
            onValueChange = { bonusText = it },
            label = { Text("Bonus") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = deductionText,
            onValueChange = { deductionText = it },
            label = { Text("Deductions") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = month,
            onValueChange = { month = it },
            label = { Text("Month (yyyy-MM) *") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { err ->
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            enabled = !isSubmitting,
            onClick = {
                val amount = amountText.toDoubleOrNull()
                val bonus = bonusText.toDoubleOrNull() ?: 0.0
                val deduction = deductionText.toDoubleOrNull() ?: 0.0

                if (source.isBlank()) {
                    errorMessage = "Source is required"
                    return@Button
                }
                if (amount == null || amount < 0) {
                    errorMessage = "Amount must be valid"
                    return@Button
                }

                isSubmitting = true
                val salId = salary?.id
                if (isEdit && salId != null) {
                    viewModel.updateSalary(
                        id = salId,
                        source = source,
                        amount = amount,
                        bonus = bonus,
                        deduction = deduction,
                        month = month,
                        note = note
                    )
                } else {
                    viewModel.addSalary(
                        source = source,
                        amount = amount,
                        bonus = bonus,
                        deduction = deduction,
                        month = month,
                        note = note
                    )
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Salary Record")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
