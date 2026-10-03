package com.KinGz.personal
import com.KinGz.personal.ui.settings.SettingsScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.KinGz.personal.data.Task
import com.KinGz.personal.ui.datausage.DataUsageViewModel
import com.KinGz.personal.ui.tasks.TaskScreen
import com.KinGz.personal.ui.tasks.TaskViewModel
import com.KinGz.personal.ui.money.MoneyScreen
import com.KinGz.personal.ui.money.MoneyViewModel
import com.KinGz.personal.ui.datausage.DataUsageScreen
import com.KinGz.personal.ui.notes.NoteViewModel
import com.KinGz.personal.ui.notes.NotesScreen
import com.KinGz.personal.ui.theme.KingzTheme
import java.util.Calendar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            KingzTheme {
                KinGzApp()
            }
        }
    }
}

data class AppModule(
    val name: String,
    val symbol: String,
    val description: String
)

private val modules = listOf(
    AppModule("Tasks", "✓", "Manage your tasks"),
    AppModule("Money", "₨", "Track your money"),
    AppModule("Notes", "✎", "Quick notes"),
    AppModule("Study", "▣", "Study material"),
    AppModule("Gym", "◆", "Track workouts"),
    AppModule("Data", "◉", "Mobile data usage"),
    AppModule("Files", "□", "Transfer files"),
    AppModule("Clipboard", "▤", "Sync clipboard"),
    AppModule("Zen", "Z", "Zen AI")
)

@Composable
fun KinGzApp() {

    var currentScreen by remember {
        mutableStateOf("dashboard")
    }

    /*
     * One TaskViewModel shared by the dashboard
     * and the full Task Manager.
     *
     * This means both screens use the same Room data.
     */
    val taskViewModel: TaskViewModel = viewModel()
    val moneyViewModel: MoneyViewModel = viewModel()
    val dataUsageViewModel : DataUsageViewModel = viewModel()
    val noteViewModel: NoteViewModel = viewModel()
    BackHandler(enabled = currentScreen != "dashboard") {
        currentScreen = "dashboard"
    }

    when (currentScreen) {

        "tasks" -> {

            TaskScreen(
                viewModel = taskViewModel,
                onBack = {
                    currentScreen = "dashboard"
                }
            )
        }

        "money" -> {

            MoneyScreen(
                viewModel = moneyViewModel,
                onBack = {
                    currentScreen = "dashboard"
                }
            )
        }
        "data" -> {

            DataUsageScreen(
                viewModel = dataUsageViewModel,
                onBack = {
                    currentScreen = "dashboard"
                }
            )
        }

        "notes" -> {

            NotesScreen(
                viewModel = noteViewModel,
                onBack = {
                    currentScreen = "dashboard"
                }
            )
        }
        "settings" -> {
            SettingsScreen(
                onBack = {
                    currentScreen = "dashboard"
                }
            )
        }



        else -> {

            Scaffold(
                topBar = {
                    KinGzTopBar(
                        onSettings = {
                            currentScreen = "settings"
                        }
                    )
                }
            ){ innerPadding ->

                Dashboard(
                    paddingValues = innerPadding,
                    taskViewModel = taskViewModel,
                    onModuleClick = { module ->

                        when (module.name) {
                            "Tasks" -> {
                                currentScreen = "tasks"
                            }

                            "Money" -> {
                                currentScreen = "money"
                            }
                            "Data"  ->  {
                                currentScreen = "data"
                            }
                            "Notes" -> {
                                currentScreen = "notes"
                            }
                        }
                    }
                )
            }
        }
    }
}
@Composable
fun KinGzTopBar(onSettings: () -> Unit) {
    SurfaceTopBar(onSettings = onSettings)
}
@Composable
fun SurfaceTopBar(onSettings: () -> Unit) {
    IconButton(onClick = onSettings) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 40.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "KinGz",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Personal Hub",
                style = MaterialTheme.typography.labelMedium
            )
        }

        androidx.compose.material3.IconButton(
            onClick = {
                // This callback must reach KinGzApp().
            }
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }

    }
}

@Composable
fun Dashboard(
    paddingValues: PaddingValues,
    taskViewModel: TaskViewModel,
    onModuleClick: (AppModule) -> Unit
) {

    val tasks by taskViewModel.tasks.collectAsState()

    val todayTasks = tasks.filter {
        !it.isCompleted && isToday(it.dueDate)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),

        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),

        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 24.dp
        ),

        verticalArrangement = Arrangement.spacedBy(14.dp),

        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item(
            span = {
                GridItemSpan(2)
            }
        ) {
            WelcomeCard()
        }

        item(
            span = {
                GridItemSpan(2)
            }
        ) {
            TodayCard(
                todayTasks = todayTasks,
                onTaskToggle = { task ->
                    taskViewModel.toggleTask(task)
                },
                onViewAll = {
                    onModuleClick(
                        AppModule(
                            name = "Tasks",
                            symbol = "✓",
                            description = "Manage your tasks"
                        )
                    )
                }
            )
        }

        items(modules) { module ->

            ModuleCard(
                module = module,
                onClick = {
                    onModuleClick(module)
                }
            )
        }
    }
}

@Composable
fun WelcomeCard() {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Welcome back, KinGz",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Your absolute dominion awaits everything you command, gathered in one place.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TodayCard(
    todayTasks: List<Task>,
    onTaskToggle: (Task) -> Unit,
    onViewAll: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onViewAll()
            },
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Today's Tasks",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = when (todayTasks.size) {
                            0 -> "Nothing scheduled for today"
                            1 -> "1 task remaining"
                            else -> "${todayTasks.size} tasks remaining"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (todayTasks.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                todayTasks
                    .take(3)
                    .forEach { task ->

                        DashboardTaskRow(
                            task = task,
                            onToggle = {
                                onTaskToggle(task)
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )
                    }

                if (todayTasks.size > 3) {

                    Text(
                        text = "+ ${todayTasks.size - 3} more task(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            top = 4.dp
                        )
                    )
                }

            } else {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Create a task with today's date and it will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardTaskRow(
    task: Task,
    onToggle: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.45f
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = {
                onToggle()
            }
        )

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration =
                    if (task.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    }
            )

            if (task.dueDate != null) {

                Text(
                    text = formatDashboardTime(task.dueDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val priorityText = when (task.priority) {
            2 -> "High"
            1 -> "Medium"
            else -> "Low"
        }

        Text(
            text = priorityText,
            style = MaterialTheme.typography.labelSmall,
            color = when (task.priority) {
                2 -> MaterialTheme.colorScheme.error
                1 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun ModuleCard(
    module: AppModule,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(20.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = module.symbol,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = module.name,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = module.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/*
 * Returns true when the task's due date
 * belongs to today's calendar date.
 */
private fun isToday(
    millis: Long?
): Boolean {

    if (millis == null) {
        return false
    }

    val today = Calendar.getInstance()

    val taskDate = Calendar.getInstance()
    taskDate.timeInMillis = millis

    return today.get(Calendar.YEAR) ==
            taskDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) ==
            taskDate.get(Calendar.DAY_OF_YEAR)
}

private fun formatDashboardTime(
    millis: Long
): String {

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis

    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val amPm = if (
        calendar.get(Calendar.AM_PM) == Calendar.AM
    ) {
        "AM"
    } else {
        "PM"
    }

    val displayHour = if (hour == 0) 12 else hour

    return String.format(
        "%d:%02d %s",
        displayHour,
        minute,
        amPm
    )
}
