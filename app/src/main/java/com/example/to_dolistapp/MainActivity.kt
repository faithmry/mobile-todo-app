package com.example.to_dolistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_dolistapp.ui.theme.ToDoListTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkByViewModel by viewModel.isDarkMode
            val darkTheme = isDarkByViewModel ?: isSystemInDarkTheme()
            ToDoListTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks
    val sortOrder by viewModel.sortOrder
    val viewMode by viewModel.viewMode
    val selectedDate by viewModel.selectedDate

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDatePickerForMode by remember { mutableStateOf(false) }

    val filteredTasks = if (viewMode == TaskViewModel.ViewMode.DAILY) {
        tasks.filter { it.deadline.toLocalDate() == selectedDate }
    } else {
        tasks
    }

    val pendingTasks = filteredTasks.filter { !it.isCompleted }
    val doneTasks = filteredTasks.filter { it.isCompleted }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (viewMode == TaskViewModel.ViewMode.DAILY) "Daily Focus" else "All My Tasks",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            val nextMode = if (viewMode == TaskViewModel.ViewMode.ALL)
                                TaskViewModel.ViewMode.DAILY else TaskViewModel.ViewMode.ALL
                            viewModel.setViewMode(nextMode)
                        }) {
                            Icon(
                                imageVector = if (viewMode == TaskViewModel.ViewMode.ALL)
                                    Icons.Default.CalendarToday else Icons.Default.FormatListBulleted,
                                contentDescription = "Switch View Mode"
                            )
                        }
                    },
                    actions = {
                        val isDark by viewModel.isDarkMode
                        IconButton(onClick = { 
                            viewModel.toggleTheme(!(isDark ?: false)) 
                        }) {
                            Icon(
                                imageVector = if (isDark == true) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Deadline") },
                                    onClick = {
                                        viewModel.setSortOrder(TaskViewModel.SortOrder.DEADLINE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Status") },
                                    onClick = {
                                        viewModel.setSortOrder(TaskViewModel.SortOrder.STATUS)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
                TaskSummaryCard(
                    todo = pendingTasks.size,
                    completed = doneTasks.size,
                    total = filteredTasks.size
                )
                if (viewMode == TaskViewModel.ViewMode.DAILY) {
                    DailyHeader(
                        date = selectedDate,
                        onClick = { showDatePickerForMode = true }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        if (filteredTasks.isEmpty()) {
            EmptyStateView(innerPadding, viewMode)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sortOrder == TaskViewModel.SortOrder.DEADLINE && viewMode == TaskViewModel.ViewMode.ALL) {
                    val groupedPending = pendingTasks.groupBy { it.deadline.toLocalDate() }.toSortedMap()
                    groupedPending.forEach { (date, tasksInGroup) ->
                        item(key = "pending_$date") {
                            SectionHeader(getLabelForDate(date), isError = date.isBefore(LocalDate.now()))
                        }
                        items(tasksInGroup, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { taskToDelete = task },
                                onClick = { taskToEdit = task }
                            )
                        }
                    }
                } else {
                    if (pendingTasks.isNotEmpty()) {
                        item(key = "header_pending") {
                            SectionHeader(if (viewMode == TaskViewModel.ViewMode.DAILY) "Tasks" else "Pending")
                        }
                        items(pendingTasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { taskToDelete = task },
                                onClick = { taskToEdit = task }
                            )
                        }
                    }
                }

                if (doneTasks.isNotEmpty()) {
                    item(key = "header_done") {
                        SectionHeader("Done", isPrimary = false)
                    }
                    items(doneTasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { taskToDelete = task },
                            onClick = { taskToEdit = task },
                            showFullDate = true
                        )
                    }
                }
            }
        }

        // Mode specific date picker
        if (showDatePickerForMode) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePickerForMode = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setSelectedDate(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePickerForMode = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerForMode = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Dialogs
        if (showAddDialog) {
            TaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, deadline ->
                    viewModel.addTask(title, deadline)
                    showAddDialog = false
                }
            )
        }
        taskToEdit?.let { task ->
            TaskDialog(
                task = task,
                onDismiss = { taskToEdit = null },
                onConfirm = { title, deadline ->
                    viewModel.updateTask(task.id, title, deadline)
                    taskToEdit = null
                }
            )
        }
        taskToDelete?.let { task ->
            DeleteConfirmDialog(
                onDismiss = { taskToDelete = null },
                onConfirm = {
                    viewModel.removeTask(task)
                    taskToDelete = null
                }
            )
        }
    }
}

@Composable
fun DailyHeader(date: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun EmptyStateView(padding: PaddingValues, viewMode: TaskViewModel.ViewMode) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (viewMode == TaskViewModel.ViewMode.DAILY) "No tasks for this day" else "No tasks yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "Tap + to add a task",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

fun getLabelForDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.plusDays(1) -> "Tomorrow"
        date.isBefore(today) -> "Past Due"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault()))
    }
}

@Composable
fun SectionHeader(label: String, isError: Boolean = false, isPrimary: Boolean = true) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = when {
                isError -> MaterialTheme.colorScheme.error
                isPrimary -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    showFullDate: Boolean = false
) {
    val isPast = task.deadline.isBefore(LocalDateTime.now()) && !task.isCompleted
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete() 
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                MaterialTheme.colorScheme.errorContainer
            } else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.dp),
            border = if (isPast) BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.8f)) else null
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                IconButton(
                    onClick = { onCheckedChange(!task.isCompleted) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (task.isCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        ),
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    val deadlineText = if (showFullDate || task.isCompleted) {
                        task.deadline.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
                    } else {
                        task.deadline.format(DateTimeFormatter.ofPattern("HH:mm"))
                    }

                    Text(
                        text = deadlineText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete Task?") },
        text = { Text("Are you sure you want to remove this task from your list?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun TaskSummaryCard(todo: Int, completed: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp) 
    ) {
        SummaryItem(label = "Active", count = todo, modifier = Modifier.weight(1f))
        SummaryItem(label = "Done", count = completed, modifier = Modifier.weight(1f))
        SummaryItem(label = "Total", count = total, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SummaryItem(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDateTime) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var selectedDate by remember { mutableStateOf(task?.deadline?.toLocalDate() ?: LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(task?.deadline?.toLocalTime()?.withSecond(0)?.withNano(0) ?: LocalTime.now().withSecond(0).withNano(0)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(20.dp))

                Text("Deadline", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Date", style = MaterialTheme.typography.labelSmall)
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Time", style = MaterialTheme.typography.labelSmall)
                            Text(selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deadline = LocalDateTime.of(selectedDate, selectedTime)
                    onConfirm(title, deadline)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(if (task == null) "Create Task" else "Save Changes")
            }
        },
        dismissButton = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}