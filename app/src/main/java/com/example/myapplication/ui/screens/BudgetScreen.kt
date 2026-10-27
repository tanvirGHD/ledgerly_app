package com.example.myapplication.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.myapplication.model.Budget
import com.example.myapplication.ui.components.ShimmerCardPlaceholder
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.LedgerlyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    authViewModel: AuthViewModel,
    viewModel: LedgerlyViewModel
) {
    val currency by authViewModel.currency.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val month by viewModel.budgetMonth.collectAsState()

    val symbol = if (currency == "BDT") "৳" else "$"

    var selectedCategory by remember { mutableStateOf("Overall") }
    var limitText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Overall", "Food", "Transport", "Rent", "Medical", "Shopping", "Bills", "Salary", "Others")

    Scaffold { paddingValues ->
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
                    text = "Budget Planner ($month)",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Set / Update Budget Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Set / Update Budget", fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Category Dropdown
                            var catExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().clickable { catExpanded = true }
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { catExpanded = true })
                                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = { selectedCategory = cat; catExpanded = false }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = limitText,
                                onValueChange = { limitText = it },
                                label = { Text("Limit ($symbol)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        errorMessage?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val limit = limitText.toDoubleOrNull()
                                if (limit == null || limit <= 0) {
                                    errorMessage = "Limit must be greater than 0"
                                    return@Button
                                }
                                errorMessage = null
                                viewModel.saveBudget(
                                    category = selectedCategory,
                                    limit = limit
                                )
                                limitText = ""
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Budget")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isInitialLoading && budgets.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShimmerCardPlaceholder(heightDp = 100)
                        ShimmerCardPlaceholder(heightDp = 100)
                    }
                } else if (budgets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No budget rules set for $month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = budgets,
                            key = { it.id ?: it.hashCode() }
                        ) { b ->
                            SwipeToDismissBudgetItem(
                                budget = b,
                                symbol = symbol,
                                onDelete = { viewModel.deleteBudget(b) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissBudgetItem(
    budget: Budget,
    symbol: String,
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
        val targetProgress = (budget.spent / budget.limit).toFloat().coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 600),
            label = "progressAnim"
        )
        val isOverLimit = budget.spent > budget.limit
        val progressColor = if (isOverLimit) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = progressColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent: $symbol${String.format("%.2f", budget.spent)}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverLimit) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                    Text("Limit: $symbol${String.format("%.2f", budget.limit)}", color = Color.Gray)
                }

                if (isOverLimit) {
                    Text(
                        text = "Over limit by $symbol${String.format("%.2f", budget.spent - budget.limit)}!",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
