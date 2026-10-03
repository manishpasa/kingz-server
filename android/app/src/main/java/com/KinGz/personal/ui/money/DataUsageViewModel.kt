package com.KinGz.personal.ui.datausage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.KinGz.personal.datausage.AppDataUsage
import com.KinGz.personal.datausage.DataUsageManager
import com.KinGz.personal.datausage.toGb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataUsageViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "data_usage_settings"
        private const val KEY_QUOTA_GB = "quota_gb"
        private const val KEY_CYCLE_START_DAY = "cycle_start_day"
    }

    private val appContext = application.applicationContext
    private val preferences =
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        DataUsageUiState(
            quotaGb = preferences.getFloat(KEY_QUOTA_GB, 0f).toDouble(),
            cycleStartDay = preferences.getInt(KEY_CYCLE_START_DAY, 1)
        )
    )

    val uiState: StateFlow<DataUsageUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun saveSettings(
        quotaGb: Double,
        cycleStartDay: Int
    ) {
        val safeQuota = quotaGb.coerceAtLeast(0.0)
        val safeDay = cycleStartDay.coerceIn(1, 31)

        preferences.edit()
            .putFloat(KEY_QUOTA_GB, safeQuota.toFloat())
            .putInt(KEY_CYCLE_START_DAY, safeDay)
            .apply()

        _uiState.value = _uiState.value.copy(
            quotaGb = safeQuota,
            cycleStartDay = safeDay,
            errorMessage = null
        )

        refresh()
    }

    fun refresh() {
        val usageAccess = DataUsageManager.hasUsageAccess(appContext)

        if (!usageAccess) {
            _uiState.value = _uiState.value.copy(
                hasUsageAccess = false,
                isLoading = false,
                errorMessage = null,
                usedBytes = 0L,
                topApps = emptyList(),
                lastUpdated = null
            )
            return
        }

        val current = System.currentTimeMillis()
        val cycleStart = DataUsageManager.calculateCycleStart(
            now = current,
            cycleStartDay = _uiState.value.cycleStartDay
        )
        val cycleEnd = DataUsageManager.calculateCycleEnd(
            cycleStart = cycleStart,
            cycleStartDay = _uiState.value.cycleStartDay
        )

        _uiState.value = _uiState.value.copy(
            hasUsageAccess = true,
            isLoading = true,
            errorMessage = null,
            cycleStart = cycleStart,
            cycleEnd = cycleEnd
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val usedBytes = DataUsageManager.readMobileUsage(
                    context = appContext,
                    startTimeMillis = cycleStart,
                    endTimeMillis = current
                )

                val topApps = DataUsageManager.readTopApps(
                    context = appContext,
                    startTimeMillis = cycleStart,
                    endTimeMillis = current,
                    limit = 10
                )

                _uiState.value = _uiState.value.copy(
                    hasUsageAccess = true,
                    isLoading = false,
                    usedBytes = usedBytes,
                    topApps = topApps,
                    lastUpdated = System.currentTimeMillis(),
                    errorMessage = null
                )
            } catch (securityException: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    hasUsageAccess = false,
                    isLoading = false,
                    errorMessage = securityException.message
                        ?: "Android did not allow access to mobile usage data.",
                    usedBytes = 0L,
                    topApps = emptyList(),
                    lastUpdated = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Could not read mobile data usage.",
                    topApps = emptyList()
                )
            }
        }
    }
}

data class DataUsageUiState(
    val hasUsageAccess: Boolean = false,
    val isLoading: Boolean = false,
    val usedBytes: Long = 0L,
    val quotaGb: Double = 0.0,
    val cycleStartDay: Int = 1,
    val cycleStart: Long = 0L,
    val cycleEnd: Long = 0L,
    val topApps: List<AppDataUsage> = emptyList(),
    val lastUpdated: Long? = null,
    val errorMessage: String? = null
) {
    val usedGb: Double
        get() = usedBytes.toGb()

    val remainingGb: Double
        get() = if (quotaGb > 0.0) {
            (quotaGb - usedGb).coerceAtLeast(0.0)
        } else {
            0.0
        }

    val quotaProgress: Float
        get() = if (quotaGb > 0.0) {
            (usedGb / quotaGb).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}
