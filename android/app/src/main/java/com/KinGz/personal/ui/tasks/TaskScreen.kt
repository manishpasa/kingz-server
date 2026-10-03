package com.KinGz.personal.ui.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.KinGz.personal.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    val tasks by viewModel.tasks.collectAsState()

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var editingTask by remember {
        mutableStateOf<Task?>(null)
    }

    var deleteTask by remember {
        mutableStateOf<Task?>(null)
    }

    val activeTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

    val overdueTasks = activeTasks.filter {
        isOverdue(it.dueDate)
    }

    val todayTasks = activeTasks.filter {
        !isOverdue(it.dueDate) && isToday(it.dueDate)
    }

    val tomorrowTasks = activeTasks.filter {
        isTomorrow(it.dueDate)
    }

    val upcomingTasks = activeTasks.filter {
        isUpcoming(it.dueDate)
    }

    val noDateTasks = activeTasks.filter {
        it.dueDate == null
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${activeTasks.size} active • ${completedTasks.size} completed",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task"
                )
            }
        }
    ) { paddingValues ->

        if (tasks.isEmpty()) {

            EmptyTaskState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (overdueTasks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Overdue",
                            count = overdueTasks.size
                        )
                    }

                    items(
                        items = overdueTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                viewModel.toggleTask(task)
                            },
                            onDelete = {
                                deleteTask = task
                            },
                            onEdit = {
                                editingTask = task
                            }
                        )
                    }
                }

                if (todayTasks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Today",
                            count = todayTasks.size
                        )
                    }

                    items(
                        items = todayTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                viewModel.toggleTask(task)
                            },
                            onDelete = {
                                deleteTask = task
                            },
                            onEdit = {
                                editingTask = task
                            }
                        )
                    }
                }

                if (tomorrowTasks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Tomorrow",
                            count = tomorrowTasks.size
                        )
                    }

                    items(
                        items = tomorrowTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                viewModel.toggleTask(task)
                            },
                            onDelete = {
                                deleteTask = task
                            },
                            onEdit = {
                                editingTask = task
                            }
                        )
                    }
                }

                if (upcomingTasks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Upcoming",
                            count = upcomingTasks.size
                        )
                    }

                    items(
                        items = upcomingTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                viewModel.toggleTask(task)
                            },
                            onDelete = {
                                deleteTask = task
                            },
                            onEdit = {
                                editingTask = task
                            }
                        )
                    }
                }

                if (noDateTasks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "No Due Date",
                            count = noDateTasks.size
                        )
                    }

                    items(
                        items = noDateTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                viewModel.toggleTask(task)
                            },
                            onDelete = {
                                deleteTask = task
                            },
                            onEdit = {
                                editingTask = task
                            }
                        )
                    }
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Completed",
                            count = completedTasks.size
                        )
                    }

                    items(
                        items = completedTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = {
                                viewModel.toggleTask(task)
                            },
                            onDelete = {
                                deleteTask = task
                            },
                            onEdit = {
                                editingTask = task
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (editingTask != null) {
        EditTaskDialog(
            task = editingTask!!,
            onDismiss = {
                editingTask = null
            },
            onSave = { updatedTask ->
                viewModel.updateTask(updatedTask)
                editingTask = null
            }
        )
    }

    if (deleteTask != null) {
        AlertDialog(
            onDismissRequest = {
                deleteTask = null
            },
            title = {
                Text(
                    text = "Delete Task?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${deleteTask!!.title}\"? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(deleteTask!!)
                        deleteTask = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTask = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = {
                showAddDialog = false
            },
            onAdd = { title, description, priority, dueDate ->
                viewModel.addTask(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate
                )

                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 8.dp,
                bottom = 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 3.dp
                )
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val priorityText = when (task.priority) {
        2 -> "High"
        1 -> "Medium"
        else -> "Low"
    }

    val priorityContainer = when (task.priority) {
        2 -> MaterialTheme.colorScheme.errorContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.45f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {

            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    onToggle()
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration =
                        if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityContainer)
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                    ) {
                        Text(
                            text = priorityText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(3.dp))

                        Text(
                            text = formatTaskDate(task.dueDate),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTaskState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.TaskAlt,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You're all caught up",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create a task and keep your day organized.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, Long?) -> Unit
) {
    var title by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var priority by remember {
        mutableStateOf(0)
    }

    var selectedDate by remember {
        mutableStateOf<Long?>(null)
    }

    var selectedHour by remember {
        mutableStateOf(18)
    }

    var selectedMinute by remember {
        mutableStateOf(0)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "New Task",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Task title")
                    },
                    placeholder = {
                        Text("e.g. Study Computer Graphics")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                    },
                    label = {
                        Text("Description")
                    },
                    placeholder = {
                        Text("Optional")
                    },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    PriorityButton(
                        text = "Low",
                        selected = priority == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        priority = 0
                    }

                    PriorityButton(
                        text = "Medium",
                        selected = priority == 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        priority = 1
                    }

                    PriorityButton(
                        text = "High",
                        selected = priority == 2,
                        modifier = Modifier.weight(1f)
                    ) {
                        priority = 2
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = selectedDate?.let {
                            formatSelectedDate(it)
                        } ?: "Choose due date"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showTimePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                        )
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    val finalDueDate =
                        if (selectedDate != null) {
                            combineDateAndTime(
                                selectedDate!!,
                                selectedHour,
                                selectedMinute
                            )
                        } else {
                            null
                        }

                    onAdd(
                        title,
                        description,
                        priority,
                        finalDueDate
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text("Create Task")
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate =
                            datePickerState.selectedDateMillis

                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
            },
            title = {
                Text("Choose time")
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember(task.id) {
        mutableStateOf(task.title)
    }

    var description by remember(task.id) {
        mutableStateOf(task.description)
    }

    var priority by remember(task.id) {
        mutableStateOf(task.priority)
    }

    var selectedDate by remember(task.id) {
        mutableStateOf(
            task.dueDate?.let {
                toDatePickerMillis(it)
            }
        )
    }

    var selectedHour by remember(task.id) {
        mutableStateOf(task.dueDate?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY)
        } ?: 18)
    }

    var selectedMinute by remember(task.id) {
        mutableStateOf(task.dueDate?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE)
        } ?: 0)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Task",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PriorityButton(
                        text = "Low",
                        selected = priority == 0,
                        modifier = Modifier.weight(1f)
                    ) { priority = 0 }

                    PriorityButton(
                        text = "Medium",
                        selected = priority == 1,
                        modifier = Modifier.weight(1f)
                    ) { priority = 1 }

                    PriorityButton(
                        text = "High",
                        selected = priority == 2,
                        modifier = Modifier.weight(1f)
                    ) { priority = 2 }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDate?.let { formatSelectedDate(it) }
                            ?: "Choose due date"
                    )
                }

                if (selectedDate != null) {
                    TextButton(
                        onClick = {
                            selectedDate = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear due date")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDueDate = if (selectedDate != null) {
                        combineDateAndTime(
                            selectedDate!!,
                            selectedHour,
                            selectedMinute
                        )
                    } else {
                        null
                    }

                    onSave(
                        task.copy(
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            dueDate = finalDueDate
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Choose time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PriorityButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = if (selected) "✓ $text" else text
        )
    }
}

private fun toDatePickerMillis(
    millis: Long
): Long {
    val localCalendar = Calendar.getInstance()
    localCalendar.timeInMillis = millis

    val utcCalendar = Calendar.getInstance(
        java.util.TimeZone.getTimeZone("UTC")
    )

    utcCalendar.clear()
    utcCalendar.set(
        localCalendar.get(Calendar.YEAR),
        localCalendar.get(Calendar.MONTH),
        localCalendar.get(Calendar.DAY_OF_MONTH),
        0,
        0,
        0
    )

    return utcCalendar.timeInMillis
}

private fun combineDateAndTime(
    dateMillis: Long,
    hour: Int,
    minute: Int
): Long {
    val selectedCalendar = Calendar.getInstance()

    val utcCalendar = Calendar.getInstance(
        java.util.TimeZone.getTimeZone("UTC")
    )

    utcCalendar.timeInMillis = dateMillis

    selectedCalendar.set(
        Calendar.YEAR,
        utcCalendar.get(Calendar.YEAR)
    )

    selectedCalendar.set(
        Calendar.MONTH,
        utcCalendar.get(Calendar.MONTH)
    )

    selectedCalendar.set(
        Calendar.DAY_OF_MONTH,
        utcCalendar.get(Calendar.DAY_OF_MONTH)
    )

    selectedCalendar.set(
        Calendar.HOUR_OF_DAY,
        hour
    )

    selectedCalendar.set(
        Calendar.MINUTE,
        minute
    )

    selectedCalendar.set(
        Calendar.SECOND,
        0
    )

    selectedCalendar.set(
        Calendar.MILLISECOND,
        0
    )

    return selectedCalendar.timeInMillis
}

private fun formatSelectedDate(
    millis: Long
): String {
    val utcCalendar = Calendar.getInstance(
        java.util.TimeZone.getTimeZone("UTC")
    )

    utcCalendar.timeInMillis = millis

    return String.format(
        Locale.getDefault(),
        "%02d/%02d/%04d",
        utcCalendar.get(Calendar.DAY_OF_MONTH),
        utcCalendar.get(Calendar.MONTH) + 1,
        utcCalendar.get(Calendar.YEAR)
    )
}

private fun formatTaskDate(
    millis: Long
): String {
    val formatter = SimpleDateFormat(
        "MMM d, h:mm a",
        Locale.getDefault()
    )

    return formatter.format(Date(millis))
}

private fun isOverdue(
    millis: Long?
): Boolean {
    return millis != null && millis < System.currentTimeMillis()
}

private fun isToday(
    millis: Long?
): Boolean {
    if (millis == null) return false

    val today = Calendar.getInstance()
    val date = Calendar.getInstance()

    date.timeInMillis = millis

    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

private fun isTomorrow(
    millis: Long?
): Boolean {
    if (millis == null) return false

    val tomorrow = Calendar.getInstance()

    tomorrow.add(
        Calendar.DAY_OF_YEAR,
        1
    )

    val date = Calendar.getInstance()

    date.timeInMillis = millis

    return tomorrow.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

private fun isUpcoming(
    millis: Long?
): Boolean {
    if (millis == null) return false

    val tomorrow = Calendar.getInstance()

    tomorrow.add(
        Calendar.DAY_OF_YEAR,
        1
    )

    val date = Calendar.getInstance()

    date.timeInMillis = millis

    return date.after(tomorrow)
}