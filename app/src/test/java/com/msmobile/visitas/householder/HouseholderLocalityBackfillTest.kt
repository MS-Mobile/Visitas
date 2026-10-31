package com.msmobile.visitas.householder

import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.AddressProvider.LocalityLookup
import com.msmobile.visitas.util.LocalityBackfillFlag
import com.msmobile.visitas.util.Logger
import com.msmobile.visitas.util.MainDispatcherRule
import com.msmobile.visitas.util.MockReferenceHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholderLocalityBackfillTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `start with the flag already set does not look anything up`() = runTest {
        // Arrange
        val addressProviderRef = MockReferenceHolder<AddressProvider>()
        val backfill = createBackfill(
            scope = this,
            isDone = true,
            householders = listOf(householder(LATITUDE_A)),
            addressProviderRef = addressProviderRef
        )

        // Act
        backfill.start().join()

        // Assert
        verify(requireNotNull(addressProviderRef.value), never()).lookupLocality(any(), any())
    }

    @Test
    fun `start without a geocoder does not look anything up nor set the flag`() = runTest {
        // Arrange
        val addressProviderRef = MockReferenceHolder<AddressProvider>()
        val flagRef = MockReferenceHolder<LocalityBackfillFlag>()
        val backfill = createBackfill(
            scope = this,
            geocoderAvailable = false,
            householders = listOf(householder(LATITUDE_A)),
            addressProviderRef = addressProviderRef,
            flagRef = flagRef
        )

        // Act
        backfill.start().join()

        // Assert
        verify(requireNotNull(addressProviderRef.value), never()).lookupLocality(any(), any())
        verify(requireNotNull(flagRef.value), never()).markDone()
    }

    @Test
    fun `start with every lookup found fills each householder and sets the flag`() = runTest {
        // Arrange
        val first = householder(LATITUDE_A)
        val second = householder(LATITUDE_B)
        val daoRef = MockReferenceHolder<HouseholderDao>()
        val flagRef = MockReferenceHolder<LocalityBackfillFlag>()
        val backfill = createBackfill(
            scope = this,
            householders = listOf(first, second),
            lookups = mapOf(
                LATITUDE_A to LocalityLookup.Found("Centro", "Criciúma"),
                LATITUDE_B to LocalityLookup.Found("Pinheirinho", "Criciúma")
            ),
            daoRef = daoRef,
            flagRef = flagRef
        )

        // Act
        backfill.start().join()

        // Assert
        val dao = requireNotNull(daoRef.value)
        verify(dao).fillLocality(first.id, LATITUDE_A, LONGITUDE, "Centro", "Criciúma")
        verify(dao).fillLocality(second.id, LATITUDE_B, LONGITUDE, "Pinheirinho", "Criciúma")
        verify(requireNotNull(flagRef.value)).markDone()
    }

    @Test
    fun `start with a failed lookup still fills the others but leaves the flag unset`() = runTest {
        // Arrange
        val failing = householder(LATITUDE_A)
        val found = householder(LATITUDE_B)
        val daoRef = MockReferenceHolder<HouseholderDao>()
        val flagRef = MockReferenceHolder<LocalityBackfillFlag>()
        val backfill = createBackfill(
            scope = this,
            householders = listOf(failing, found),
            lookups = mapOf(
                LATITUDE_A to LocalityLookup.Failed,
                LATITUDE_B to LocalityLookup.Found("Centro", "Criciúma")
            ),
            daoRef = daoRef,
            flagRef = flagRef
        )

        // Act
        backfill.start().join()

        // Assert
        val dao = requireNotNull(daoRef.value)
        verify(dao).fillLocality(found.id, LATITUDE_B, LONGITUDE, "Centro", "Criciúma")
        verify(dao, never()).fillLocality(eq(failing.id), any(), any(), anyOrNull(), anyOrNull())
        verify(requireNotNull(flagRef.value), never()).markDone()
    }

    @Test
    fun `start with an answer lacking a neighborhood fills nulls and sets the flag`() = runTest {
        // Arrange
        val householder = householder(LATITUDE_A)
        val daoRef = MockReferenceHolder<HouseholderDao>()
        val flagRef = MockReferenceHolder<LocalityBackfillFlag>()
        val backfill = createBackfill(
            scope = this,
            householders = listOf(householder),
            lookups = mapOf(LATITUDE_A to LocalityLookup.Found(null, "Criciúma")),
            daoRef = daoRef,
            flagRef = flagRef
        )

        // Act
        backfill.start().join()

        // Assert
        verify(requireNotNull(daoRef.value))
            .fillLocality(householder.id, LATITUDE_A, LONGITUDE, null, "Criciúma")
        verify(requireNotNull(flagRef.value)).markDone()
    }

    @Test
    fun `start when the database fails logs the error and leaves the flag unset`() = runTest {
        // Arrange
        val loggerRef = MockReferenceHolder<Logger>()
        val flagRef = MockReferenceHolder<LocalityBackfillFlag>()
        val backfill = createBackfill(
            scope = this,
            listFails = true,
            loggerRef = loggerRef,
            flagRef = flagRef
        )

        // Act
        backfill.start().join()

        // Assert
        verify(requireNotNull(loggerRef.value)).error(any(), any(), any())
        verify(requireNotNull(flagRef.value), never()).markDone()
    }

    @Test
    fun `start while a run is active returns that run instead of starting another`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        val addressProviderRef = MockReferenceHolder<AddressProvider>()
        val backfill = createBackfill(
            scope = this,
            householders = listOf(householder(LATITUDE_A)),
            lookups = mapOf(LATITUDE_A to LocalityLookup.Found("Centro", "Criciúma")),
            lookupGate = gate,
            addressProviderRef = addressProviderRef
        )
        val firstRun = backfill.start()
        runCurrent()

        // Act
        val secondRun = backfill.start()
        gate.complete(Unit)
        secondRun.join()

        // Assert
        assertSame(firstRun, secondRun)
        verify(requireNotNull(addressProviderRef.value), times(1)).lookupLocality(any(), any())
    }

    @Test
    fun `restart while a run is active cancels it, clears the flag and runs again`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        val householder = householder(LATITUDE_A)
        val addressProviderRef = MockReferenceHolder<AddressProvider>()
        val daoRef = MockReferenceHolder<HouseholderDao>()
        val flagRef = MockReferenceHolder<LocalityBackfillFlag>()
        val backfill = createBackfill(
            scope = this,
            householders = listOf(householder),
            lookups = mapOf(LATITUDE_A to LocalityLookup.Found("Centro", "Criciúma")),
            lookupGate = gate,
            addressProviderRef = addressProviderRef,
            daoRef = daoRef,
            flagRef = flagRef
        )
        val firstRun = backfill.start()
        runCurrent()

        // Act
        val secondRun = backfill.restart()
        runCurrent()
        gate.complete(Unit)
        secondRun.join()

        // Assert
        assertTrue(firstRun.isCancelled)
        val flag = requireNotNull(flagRef.value)
        verify(flag).clear()
        verify(flag).markDone()
        verify(requireNotNull(addressProviderRef.value), times(2)).lookupLocality(any(), any())
        verify(requireNotNull(daoRef.value), times(1))
            .fillLocality(householder.id, LATITUDE_A, LONGITUDE, "Centro", "Criciúma")
    }

    private fun createBackfill(
        scope: CoroutineScope,
        isDone: Boolean = false,
        geocoderAvailable: Boolean = true,
        householders: List<Householder> = emptyList(),
        lookups: Map<Double, LocalityLookup> = emptyMap(),
        lookupGate: CompletableDeferred<Unit>? = null,
        listFails: Boolean = false,
        addressProviderRef: MockReferenceHolder<AddressProvider>? = null,
        daoRef: MockReferenceHolder<HouseholderDao>? = null,
        flagRef: MockReferenceHolder<LocalityBackfillFlag>? = null,
        loggerRef: MockReferenceHolder<Logger>? = null
    ): HouseholderLocalityBackfill {
        val dao = mock<HouseholderDao> {
            if (listFails) {
                onBlocking { listMissingLocality() } doAnswer { throw IllegalStateException("db") }
            } else {
                onBlocking { listMissingLocality() } doReturn householders
            }
        }
        val addressProvider = mock<AddressProvider> {
            on { isGeocoderAvailable() } doReturn geocoderAvailable
            onBlocking { lookupLocality(any(), any()) } doSuspendableAnswer { invocation ->
                lookupGate?.await()
                lookups.getValue(invocation.getArgument(0))
            }
        }
        val flag = mock<LocalityBackfillFlag> {
            on { isDone() } doReturn isDone
        }
        val logger = mock<Logger>()
        addressProviderRef?.value = addressProvider
        daoRef?.value = dao
        flagRef?.value = flag
        loggerRef?.value = logger
        return HouseholderLocalityBackfill(
            householderDao = dao,
            addressProvider = addressProvider,
            backfillFlag = flag,
            applicationScope = scope,
            logger = logger
        )
    }

    private fun householder(latitude: Double) = Householder(
        id = UUID.randomUUID(),
        name = "Householder $latitude",
        address = "Rua Test, 1",
        notes = null,
        addressLatitude = latitude,
        addressLongitude = LONGITUDE
    )

    private companion object {
        private const val LATITUDE_A = -28.6775
        private const val LATITUDE_B = -28.7000
        private const val LONGITUDE = -49.3697
    }
}
