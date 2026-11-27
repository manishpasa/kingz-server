package com.KinGz.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.KinGz.personal.data.Task
import com.KinGz.personal.network.AuthPreferences
import com.KinGz.personal.network.AuthRepository
import com.KinGz.personal.network.UserResponse
import com.KinGz.personal.ui.auth.AuthScreen
import com.KinGz.personal.ui.datausage.DataUsageScreen
import com.KinGz.personal.ui.datausage.DataUsageViewModel
import com.KinGz.personal.ui.money.MoneyScreen
import com.KinGz.personal.ui.money.MoneyViewModel
import com.KinGz.personal.ui.notes.NoteViewModel
import com.KinGz.personal.ui.notes.NotesScreen
import com.KinGz.personal.ui.settings.SettingsScreen
import com.KinGz.personal.ui.tasks.TaskScreen
import com.KinGz.personal.ui.tasks.TaskViewModel
import com.KinGz.personal.ui.theme.KingzTheme
import java.util.Calendar

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
    val icon: ImageVector,
    val description: String
)

private val modules = listOf(
    AppModule("Tasks", Icons.Filled.TaskAlt, "Manage your tasks"),
    AppModule("Money", Icons.Filled.AccountBalanceWallet, "Track your money"),
    AppModule("Notes", Icons.Filled.StickyNote2, "Quick notes"),
    AppModule("Data", Icons.Filled.DataUsage, "Mobile data usage"),
    AppModule("KinGz Server", Icons.Filled.Cloud, "Your private server and files")
)

@Composable
fun KinGzApp() {
    val context = LocalContext.current
    val authRepository = remember {
        AuthRepository(context.applicationContext)
    }

    var isAdminUser by remember { mutableStateOf(false) }

    var username by remember {
        mutableStateOf(
            AuthPreferences(context.applicationContext)
                .getUsername() ?: "Unknown"
        )
    }

    var patternUnlocked by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("dashboard") }

    val isPatternSet = remember {
        LocalPatternStore.isPatternSet(context)
    }

    val taskViewModel: TaskViewModel = viewModel()
    val moneyViewModel: MoneyViewModel = viewModel()
    val dataUsageViewModel: DataUsageViewModel = viewModel()
    val noteViewModel: NoteViewModel = viewModel()

    BackHandler(
        enabled = patternUnlocked && currentScreen != "dashboard"
    ) {
        currentScreen = when (currentScreen) {
            "file-storage" -> "server"
            "admin" -> "server"
            else -> "dashboard"
        }
    }

    if (!patternUnlocked) {
        PatternLockScreen(
            isSetup = !isPatternSet,
            onUnlocked = {
                patternUnlocked = true
                currentScreen = "dashboard"
            }
        )
        return
    }

    when (currentScreen) {
        "tasks" -> {
            TaskScreen(
                viewModel = taskViewModel,
                onBack = { currentScreen = "dashboard" }
            )
        }

        "money" -> {
            MoneyScreen(
                viewModel = moneyViewModel,
                onBack = { currentScreen = "dashboard" }
            )
        }

        "data" -> {
            DataUsageScreen(
                viewModel = dataUsageViewModel,
                onBack = { currentScreen = "dashboard" }
            )
        }

        "notes" -> {
            NotesScreen(
                viewModel = noteViewModel,
                onBack = { currentScreen = "dashboard" }
            )
        }

        "settings" -> {
            SettingsScreen(
                onBack = { currentScreen = "dashboard" }
            )
        }

        "server-check" -> {
            ServerSessionCheck(
                repository = authRepository,
                onAuthenticated = { user ->
                    username = user.username
                    isAdminUser = user.isAdmin
                    currentScreen = "server"
                },
                onUnauthenticated = {
                    isAdminUser = false
                    currentScreen = "auth"
                }
            )
        }

        "auth" -> {
            AuthScreen(
                onAuthenticated = { user ->
                    username = user.username
                    isAdminUser = user.isAdmin
                    currentScreen = "server"
                }
            )
        }

        "server" -> {
            ServerHomeScreen(
                username = username,
                isAdmin = isAdminUser,
                onBack = { currentScreen = "dashboard" },
                onFileStorage = { currentScreen = "file-storage" },
                onAdminPanel = {
                    if (isAdminUser) {
                        currentScreen = "admin"
                    } else {
                        currentScreen = "server-check"
                    }
                }
            )
        }

        "file-storage" -> {
            FileStorageContent(
                onBack = { currentScreen = "server" }
            )
        }

        "admin" -> {
            if (isAdminUser) {
                AdminScreen(
                    onBack = { currentScreen = "server" }
                )
            } else {
                ServerSessionCheck(
                    repository = authRepository,
                    onAuthenticated = { user ->
                        username = user.username
                        isAdminUser = user.isAdmin
                        currentScreen = if (user.isAdmin) {
                            "admin"
                        } else {
                            "server"
                        }
                    },
                    onUnauthenticated = {
                        isAdminUser = false
                        currentScreen = "auth"
                    }
                )
            }
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
            ) { innerPadding ->
                Dashboard(
                    paddingValues = innerPadding,
                    taskViewModel = taskViewModel,
                    username = username,
                    onModuleClick = { module ->
                        when (module.name) {
                            "Tasks" -> currentScreen = "tasks"
                            "Money" -> currentScreen = "money"
                            "Data" -> currentScreen = "data"
                            "Notes" -> currentScreen = "notes"
                            "KinGz Server" -> currentScreen = "server-check"
                        }
                    }
                )
            }
        }
    }
}
@Composable
private fun ServerSessionCheck(
    repository: AuthRepository,
    onAuthenticated: (UserResponse) -> Unit,
    onUnauthenticated: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!repository.isLoggedIn()) {
            onUnauthenticated()
            return@LaunchedEffect
        }

        val result = repository.getCurrentUser()
        val user = result.getOrNull()

        if (user != null) {
            // Both normal users and admins may enter KinGz Server.
            onAuthenticated(user)
        } else {
            onUnauthenticated()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ServerHomeScreen(
    username: String,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onFileStorage: () -> Unit,
    onAdminPanel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("ΓåÉ Back to dashboard")
        }

        Text(
            text = "KinGz Server",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Signed in as $username",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "File Storage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Access your private server files. Upload, browse, download, and retrieve files from your KinGz server."
                )

                Button(
                    onClick = onFileStorage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open File Storage")
                }
            }
        }

        if (isAdmin) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Admin Panel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Manage authorized devices, sessions, and server health.")

                    Button(
                        onClick = onAdminPanel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Admin Panel")
                    }
                }
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
        Column(modifier = Modifier.weight(1f)) {
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

        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
fun Dashboard(
    paddingValues: PaddingValues,
    taskViewModel: TaskViewModel,
    username: String,
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
        item(span = { GridItemSpan(2) }) {
            WelcomeCard(username)
        }

        item(span = { GridItemSpan(2) }) {
            TodayCard(
                todayTasks = todayTasks,
                onTaskToggle = { task ->
                    taskViewModel.toggleTask(task)
                },
                onViewAll = {
                    onModuleClick(
                        AppModule(
                            name = "Tasks",
                            icon = Icons.Filled.TaskAlt,
                            description = "Manage your tasks"
                        )
                    )
                }
            )
        }

        items(modules) { module ->
            ModuleCard(
                module = module,
                onClick = { onModuleClick(module) }
            )
        }
    }
}

@Composable
fun WelcomeCard(username: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Welcome back, $username",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

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
            .clickable { onViewAll() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                    Icon(imageVector = Icons.Filled.TaskAlt, contentDescription = "Today tasks", modifier = Modifier.size(30.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
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

                Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "View tasks")
            }

            if (todayTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                todayTasks.take(3).forEach { task ->
                    DashboardTaskRow(
                        task = task,
                        onToggle = { onTaskToggle(task) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
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
                Spacer(modifier = Modifier.height(12.dp))

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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
            onCheckedChange = { onToggle() }
        )

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) {
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
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Icon(imageVector = module.icon, contentDescription = module.name, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = module.name,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = module.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/*
 * Returns true when the task's due date belongs to today's calendar date.
 */
private fun isToday(millis: Long?): Boolean {
    if (millis == null) return false

    val today = Calendar.getInstance()
    val taskDate = Calendar.getInstance()
    taskDate.timeInMillis = millis

    return today.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR)
}

private fun formatDashboardTime(millis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis

    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
        "AM"
    } else {
        "PM"
    }

    val displayHour = if (hour == 0) 12 else hour

    return String.format("%d:%02d %s", displayHour, minute, amPm)
}
