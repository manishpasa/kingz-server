package com.KinGz.personal.datausage

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.provider.Settings
import java.util.Calendar
import java.util.Locale

/**
 * Reads cellular usage from Android's NetworkStats service.
 *
 * This is read-only: the app does not change Android's system data counter.
 */
object DataUsageManager {

    const val BYTES_PER_GB = 1_000_000_000L

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettingsIntent(context: Context) =
        android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun readMobileUsage(
        context: Context,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Long {
        val networkStatsManager =
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val bucket = networkStatsManager.querySummaryForUser(
            ConnectivityManager.TYPE_MOBILE,
            null,
            startTimeMillis,
            endTimeMillis
        ) ?: return 0L

        return (bucket.rxBytes + bucket.txBytes).coerceAtLeast(0L)
    }

    fun readTopApps(
        context: Context,
        startTimeMillis: Long,
        endTimeMillis: Long,
        limit: Int = 10
    ): List<AppDataUsage> {
        val networkStatsManager =
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val stats = networkStatsManager.querySummary(
            ConnectivityManager.TYPE_MOBILE,
            null,
            startTimeMillis,
            endTimeMillis
        )

        val totalsByUid = mutableMapOf<Int, Long>()
        val bucket = NetworkStats.Bucket()

        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket)

            if (bucket.uid < 0) {
                continue
            }

            val bytes = (bucket.rxBytes + bucket.txBytes).coerceAtLeast(0L)
            if (bytes > 0L) {
                totalsByUid[bucket.uid] =
                    (totalsByUid[bucket.uid] ?: 0L) + bytes
            }
        }

        return totalsByUid.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (uid, bytes) ->
                resolveAppDataUsage(context, uid, bytes)
            }
    }

    private fun resolveAppDataUsage(
        context: Context,
        uid: Int,
        bytes: Long
    ): AppDataUsage {
        val packageManager = context.packageManager
        val packageName = packageManager
            .getPackagesForUid(uid)
            ?.firstOrNull()

        if (packageName == null) {
            return AppDataUsage(
                label = "UID $uid",
                packageName = "Unknown package",
                bytes = bytes
            )
        }

        val label = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) {
            packageName
        }

        return AppDataUsage(
            label = label,
            packageName = packageName,
            bytes = bytes
        )
    }

    fun calculateCycleStart(
        now: Long,
        cycleStartDay: Int
    ): Long {
        val current = Calendar.getInstance()
        current.timeInMillis = now

        val candidate = current.clone() as Calendar
        candidate.set(
            Calendar.DAY_OF_MONTH,
            minOf(cycleStartDay, current.getActualMaximum(Calendar.DAY_OF_MONTH))
        )
        candidate.set(Calendar.HOUR_OF_DAY, 0)
        candidate.set(Calendar.MINUTE, 0)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)

        if (candidate.timeInMillis > now) {
            candidate.add(Calendar.MONTH, -1)
            candidate.set(
                Calendar.DAY_OF_MONTH,
                minOf(cycleStartDay, candidate.getActualMaximum(Calendar.DAY_OF_MONTH))
            )
        }

        return candidate.timeInMillis
    }

    fun calculateCycleEnd(
        cycleStart: Long,
        cycleStartDay: Int
    ): Long {
        val end = Calendar.getInstance()
        end.timeInMillis = cycleStart
        end.add(Calendar.MONTH, 1)
        end.set(
            Calendar.DAY_OF_MONTH,
            minOf(cycleStartDay, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        )
        end.set(Calendar.HOUR_OF_DAY, 0)
        end.set(Calendar.MINUTE, 0)
        end.set(Calendar.SECOND, 0)
        end.set(Calendar.MILLISECOND, 0)
        return end.timeInMillis
    }

    fun formatDate(millis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis

        return String.format(
            Locale.getDefault(),
            "%d %s %d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.getDisplayName(
                Calendar.MONTH,
                Calendar.SHORT,
                Locale.getDefault()
            ),
            calendar.get(Calendar.YEAR)
        )
    }
}

data class AppDataUsage(
    val label: String,
    val packageName: String,
    val bytes: Long
)

fun Long.toGb(): Double = this.toDouble() / DataUsageManager.BYTES_PER_GB
