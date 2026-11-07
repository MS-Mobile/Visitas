package com.msmobile.visitas.routing

import com.msmobile.visitas.util.VisitMapAdapter
import com.msmobile.visitas.visit.VisitMapData
import com.squareup.moshi.Moshi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class OsrmModelsTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun `route response parses geojson geometry`() {
        val response = moshi.adapter(OsrmRouteResponse::class.java).fromJson(ROUTE_RESPONSE_JSON)

        val geometry = response?.routes?.firstOrNull()?.geometry
        assertEquals("LineString", geometry?.type)
        assertEquals(
            listOf(listOf(-49.369954, -28.679819), listOf(-49.359789, -28.670001)),
            geometry?.coordinates
        )
    }

    @Test
    fun `map data serializes route geometry as a geojson object`() {
        val geometry = OsrmGeometry(
            type = "LineString",
            coordinates = listOf(listOf(-49.369954, -28.679819), listOf(-49.359789, -28.670001))
        )
        val mapData = VisitMapData(
            householderName = "Name",
            visitSubject = "Subject",
            householderAddress = "Address",
            householderLatitude = -28.670001,
            householderLongitude = -49.359789,
            householderDistance = null,
            visitOrder = 0,
            routeGeometry = geometry
        )

        val json = VisitMapAdapter(moshi).toJson(listOf(mapData)).orEmpty()

        assertTrue(json, json.contains("\"routeGeometry\":{\"type\":\"LineString\",\"coordinates\":[["))
    }

    private companion object {
        // Trimmed from a real router.project-osrm.org response for overview=full&geometries=geojson&steps=false
        const val ROUTE_RESPONSE_JSON = """
            {
              "code": "Ok",
              "routes": [{
                "legs": [{"steps": [], "weight": 251.1, "summary": "", "duration": 251.1, "distance": 2481}],
                "weight_name": "routability",
                "geometry": {
                  "coordinates": [[-49.369954, -28.679819], [-49.359789, -28.670001]],
                  "type": "LineString"
                },
                "weight": 251.1,
                "duration": 251.1,
                "distance": 2481
              }],
              "waypoints": [
                {"hint": "qV6Jjv", "location": [-49.369954, -28.679819], "name": "Avenida Centenário", "distance": 20.55},
                {"hint": "yxb4hP", "location": [-49.359789, -28.670001], "name": "Rua Elaine", "distance": 20.62}
              ]
            }
        """
    }
}
