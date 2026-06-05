package com.msmobile.visitas.preference

import com.msmobile.visitas.visit.VisitMapEngineOption
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PreferenceTypeConvertersTest {
    private val converters = PreferenceTypeConverters()

    @Test
    fun `fromMapEngineOption returns enum name`() {
        assertEquals("MapLibre", converters.fromMapEngineOption(VisitMapEngineOption.MapLibre))
        assertEquals("Leaflet", converters.fromMapEngineOption(VisitMapEngineOption.Leaflet))
    }

    @Test
    fun `toMapEngineOption parses known values`() {
        assertEquals(VisitMapEngineOption.MapLibre, converters.toMapEngineOption("MapLibre"))
        assertEquals(VisitMapEngineOption.Leaflet, converters.toMapEngineOption("Leaflet"))
    }

    @Test
    fun `toMapEngineOption falls back to MapLibre for unknown value`() {
        assertEquals(VisitMapEngineOption.MapLibre, converters.toMapEngineOption("unknown"))
    }
}
