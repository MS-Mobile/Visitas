package com.msmobile.visitas.householder

import com.msmobile.visitas.di.ApplicationScope
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.AddressProvider.LocalityLookup
import com.msmobile.visitas.util.LocalityBackfillFlag
import com.msmobile.visitas.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reverse-geocodes the neighborhood and city of householders saved with coordinates but without
 * them (everything saved before the geocoder kept them, and older backups).
 *
 * [LocalityBackfillFlag] is set only by a run in which every lookup got an answer, so a failed
 * lookup (offline, timeout) makes the next launch go through the remaining householders again. A
 * householder the geocoder answers without a neighborhood stays a candidate, but runs only repeat
 * while some lookup keeps failing.
 */
@Singleton
class HouseholderLocalityBackfill @Inject constructor(
    private val householderDao: HouseholderDao,
    private val addressProvider: AddressProvider,
    private val backfillFlag: LocalityBackfillFlag,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    private val logger: Logger
) {
    private val lock = Any()
    private var activeRun: Job? = null

    /** Starts a run unless one is in progress; returns the run either way. */
    fun start(): Job = synchronized(lock) {
        activeRun?.takeIf { it.isActive } ?: launchRun(previousRun = null, clearFlag = false)
    }

    /**
     * Replaces any run in progress with a fresh one over the current data, ignoring the flag. Used
     * after a restore: a run that began before it would miss the restored householders and could
     * still set the flag.
     */
    fun restart(): Job = synchronized(lock) {
        launchRun(previousRun = activeRun, clearFlag = true)
    }

    private fun launchRun(previousRun: Job?, clearFlag: Boolean): Job {
        return applicationScope.launch {
            previousRun?.cancelAndJoin()
            if (clearFlag) backfillFlag.clear()
            backfill()
        }.also { activeRun = it }
    }

    private suspend fun backfill() {
        try {
            if (backfillFlag.isDone()) return
            if (!addressProvider.isGeocoderAvailable()) return
            var anyLookupFailed = false
            householderDao.listMissingLocality().forEach { householder ->
                val latitude = householder.addressLatitude ?: return@forEach
                val longitude = householder.addressLongitude ?: return@forEach
                when (val lookup = addressProvider.lookupLocality(latitude, longitude)) {
                    is LocalityLookup.Found -> householderDao.fillLocality(
                        id = householder.id,
                        latitude = latitude,
                        longitude = longitude,
                        neighborhood = lookup.neighborhood,
                        city = lookup.city
                    )
                    LocalityLookup.Failed -> anyLookupFailed = true
                }
            }
            if (!anyLookupFailed) backfillFlag.markDone()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error(TAG, "Householder locality backfill failed", error)
        }
    }

    private companion object {
        private const val TAG = "HouseholderLocalityBackfill"
    }
}
