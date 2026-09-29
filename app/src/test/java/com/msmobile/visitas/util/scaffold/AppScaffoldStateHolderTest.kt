package com.msmobile.visitas.util.scaffold

import com.msmobile.visitas.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.UUID

class AppScaffoldStateHolderTest {
    private val detail = AppDestination.VisitDetail(householderId = UUID(0, 1))
    private val list = AppDestination.VisitList
    private val detailChrome = AppScaffoldState.UiState(subtitle = "detail")
    private val listChrome = AppScaffoldState.UiState(subtitle = "list")

    @Test
    fun `stateFor returns the same state for the same destination`() {
        // Arrange
        val holder = AppScaffoldStateHolder()

        // Act
        val first = holder.stateFor(detail)
        val second = holder.stateFor(detail)

        // Assert
        assertSame(first, second)
    }

    @Test
    fun `screen below publishing during predictive back does not replace the top screen chrome`() {
        // Arrange
        val holder = AppScaffoldStateHolder()
        val detailOwner = Any()
        val listOwner = Any()
        holder.stateFor(detail).setUiState(detailOwner, detailChrome)

        // Act
        holder.stateFor(list).setUiState(listOwner, listChrome)

        // Assert
        assertEquals(detailChrome, holder.stateFor(detail).uiState)
    }

    @Test
    fun `cancelled predictive back keeps the top screen chrome`() {
        // Arrange
        val holder = AppScaffoldStateHolder()
        val detailOwner = Any()
        val listOwner = Any()
        holder.stateFor(detail).setUiState(detailOwner, detailChrome)
        holder.stateFor(list).setUiState(listOwner, listChrome)

        // Act
        holder.stateFor(list).clearUiState(listOwner)

        // Assert
        assertEquals(detailChrome, holder.stateFor(detail).uiState)
    }

    @Test
    fun `retainOnly drops states of destinations no longer on a back stack`() {
        // Arrange
        val holder = AppScaffoldStateHolder()
        val listState = holder.stateFor(list)
        val detailState = holder.stateFor(detail)

        // Act
        holder.retainOnly(listOf(list))

        // Assert
        assertSame(listState, holder.stateFor(list))
        assertNotSame(detailState, holder.stateFor(detail))
    }
}
