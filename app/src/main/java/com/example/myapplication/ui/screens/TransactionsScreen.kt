package com.example.myapplication.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.myapplication.model.Transaction
import com.example.myapplication.ui.components.ShimmerCardPlaceholder
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.LedgerlyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    authViewModel: AuthViewModel,
    viewModel: LedgerlyViewModel
) {
    val currency by authViewModel.currency.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val symbol = if (currency == "BDT") "৳" else "$"

    // State for ModalBottomSheet Form
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val search by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.selectedType.collectAsState()
    val categoryFilter by viewModel.selectedCategory.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransaction = null
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                // Search Input
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        viewModel.searchQuery.value = it
                    },
                    label = { Text("Search transactions...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Type Chips Filter (All / Income / Expense)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("All", "Income", "Expense").forEach { type ->
                        FilterChip(
                            selected = (typeFilter == type),
                            onClick = {
                                viewModel.selectedType.value = type
                            },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.categories) { cat ->
                        FilterChip(
                            selected = (categoryFilter == cat),
                            onClick = {
                                viewModel.selectedCategory.value = cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isInitialLoading && transactions.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShimmerCardPlaceholder(heightDp = 70)
                        ShimmerCardPlaceholder(heightDp = 70)
                        ShimmerCardPlaceholder(heightDp = 70)
                    }
                } else {
                    val filteredList = transactions.filter { tx ->
                        val matchesType = when (typeFilter) {
                            "Income" -> tx.type.equals("income", ignoreCase = true)
                            "Expense" -> tx.type.equals("expense", ignoreCase = true)
                            else -> true
                        }
                        val matchesCat = (categoryFilter == "All" || tx.category.equals(categoryFilter, ignoreCase = true))
                        val matchesSearch = search.isBlank() || (tx.note?.contains(search, ignoreCase = true) == true || tx.category.contains(search, ignoreCase = true))
                        matchesType && matchesCat && matchesSearch
                    }

                    val totalIncome = filteredList.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
                    val totalExpense = filteredList.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }

                    // Header Summary
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text("${filteredList.size} Items", fontWeight = FontWeight.Bold)
                            Text("+$symbol${String.format("%.2f", totalIncome)}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            Text("-$symbol${String.format("%.2f", totalExpense)}", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(
                                items = filteredList,
                                key = { it.id ?: it.hashCode() }
                            ) { tx ->
                                SwipeToDismissTransactionItem(
                                    tx = tx,
                                    symbol = symbol,
                                    onEdit = {
                                        editingTransaction = tx
                                        showBottomSheet = true
                                    },
                                    onDelete = { viewModel.deleteTransaction(tx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Form
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            TransactionFormBottomSheet(
                transaction = editingTransaction,
                viewModel = viewModel,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissTransactionItem(
    tx: Transaction,
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
        val isIncome = tx.type.equals("income", ignoreCase = true)
        val color = if (isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
        val sign = if (isIncome) "+" else "-"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .clickable { onEdit() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = tx.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = tx.method.uppercase(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = tx.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (!tx.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = tx.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Text(
                    text = "$sign$symbol${String.format("%.2f", tx.amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}

@Composable
fun TransactionFormBottomSheet(
    transaction: Transaction?,
    viewModel: LedgerlyViewModel,
    defaultType: String = "expense",
    onDismiss: () -> Unit
) {
    val isEdit = transaction != null

    var type by remember { mutableStateOf(transaction?.type ?: defaultType) }
    var amountText by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(transaction?.category ?: "Food") }
    var method by remember { mutableStateOf(transaction?.method ?: "cash") }
    var date by remember { mutableStateOf(transaction?.date ?: viewModel.getTodayDate()) }
    var note by remember { mutableStateOf(transaction?.note ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isEdit) "Edit Transaction" else "New Transaction",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Type Selector
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { type = "income" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "income") Color(0xFF10B981) else Color.Gray.copy(alpha = 0.3f)
                ),
                modifier = Modifier.weight(1f)
            ) { Text("Income") }

            Button(
                onClick = { type = "expense" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "expense") Color(0xFFEF4444) else Color.Gray.copy(alpha = 0.3f)
                ),
                modifier = Modifier.weight(1f)
            ) { Text("Expense") }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Category Selection
        Text("Category", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.categories.filter { it != "All" }) { cat ->
                FilterChip(
                    selected = (category == cat),
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        // Method Selection
        Text("Payment Method", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.methods) { m ->
                FilterChip(
                    selected = (method == m),
                    onClick = { method = m },
                    label = { Text(m.uppercase()) }
                )
            }
        }

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (yyyy-MM-dd) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { err ->
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            enabled = !isSubmitting,
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    errorMessage = "Amount must be greater than 0"
                    return@Button
                }
                if (date.isBlank()) {
                    errorMessage = "Date is required"
                    return@Button
                }

                isSubmitting = true
                if (isEdit && transaction?.id != null) {
                    viewModel.updateTransaction(
                        id = transaction.id,
                        type = type,
                        amount = amount,
                        category = category,
                        method = method,
                        date = date,
                        note = note
                    )
                } else {
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        category = category,
                        method = method,
                        date = date,
                        note = note
                    )
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Transaction")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
