package com.example.bloodbank.domain.model

import kotlin.random.Random

data class HospitalMarker(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

object MockHospitalData {
    val hospitals: List<HospitalMarker> by lazy {
        val list = mutableListOf(
            // NCR
            HospitalMarker("h1", "Philippine Red Cross - National Blood Center", "Port Area, Manila", 14.5878, 120.9754),
            HospitalMarker("h2", "Philippine General Hospital", "Taft Ave, Manila", 14.5794, 120.9850),
            HospitalMarker("h3", "Makati Medical Center", "Amorsolo St, Makati", 14.5592, 121.0142),
            HospitalMarker("h4", "St. Luke's Medical Center", "E Rodriguez Sr. Ave, Quezon City", 14.6225, 121.0238),
            // Luzon
            HospitalMarker("h5", "Baguio General Hospital", "Gov. Pack Rd, Baguio City", 16.4023, 120.5960),
            HospitalMarker("h6", "Bicol Medical Center", "Panganiban Dr, Naga City", 13.6218, 123.1838),
            // Visayas
            HospitalMarker("h7", "Vicente Sotto Memorial Medical Center", "B. Rodriguez St, Cebu City", 10.3096, 123.8934),
            HospitalMarker("h8", "Corazon Locsin Montelibano Regional Hospital", "Lacson St, Bacolod City", 10.6765, 122.9509),
            HospitalMarker("h9", "Western Visayas Medical Center", "Mandurriao, Iloilo City", 10.7139, 122.5484),
            // Mindanao
            HospitalMarker("h10", "Southern Philippines Medical Center", "J.P. Laurel Ave, Davao City", 7.0935, 125.6133),
            HospitalMarker("h11", "Northern Mindanao Medical Center", "Capitol Comp, Cagayan de Oro", 8.4815, 124.6473),
            HospitalMarker("h12", "Zamboanga City Medical Center", "Dr. D. Evangelista St, Zamboanga", 6.9054, 122.0792)
        )

        // Generate 88 more hospitals randomly distributed across the Philippines
        val cities = listOf("Cebu", "Davao", "Manila", "Quezon", "Iloilo", "Bacolod", "Cagayan de Oro", "Zamboanga", "General Santos", "Makati", "Pasig", "Taguig", "Antipolo", "Dasmarinas", "Valenzuela", "Bacoor", "Muntinlupa", "San Jose", "Las Pinas", "Baguio", "Lapu-Lapu", "Iligan", "Mandaue", "Caloocan", "Marikina", "Pasay", "Tuguegarao", "Laoag", "Vigan", "San Fernando", "Angeles", "Olongapo", "Tarlac", "Cabanatuan", "Malolos", "San Pablo", "Lucena", "Batangas", "Lipa", "Naga", "Legazpi", "Sorsogon", "Ormoc", "Tacloban", "Tagbilaran", "Dumaguete", "Roxas", "Kalibo", "Butuan", "Surigao", "Pagadian", "Dipolog", "Cotabato", "Kidapawan", "Koronadal", "Digos", "Mati", "Tagum")
        val types = listOf("Medical Center", "General Hospital", "Provincial Hospital", "Memorial Hospital", "Regional Medical Center", "City Hospital", "Doctors' Hospital", "Polyclinic", "Blood Bank Center")

        val random = Random(42) // Fixed seed so they stay in the same place every time
        for (i in 13..100) {
            // Philippines approx bounding box: Lat (5.0 to 19.0), Lng (117.0 to 126.0)
            val randomLat = 6.0 + random.nextDouble() * (18.0 - 6.0)
            val randomLng = 119.0 + random.nextDouble() * (126.0 - 119.0)
            
            val city = cities.random(random)
            val type = types.random(random)
            
            list.add(
                HospitalMarker(
                    id = "h$i",
                    name = "$city $type",
                    address = "$city City, Philippines",
                    latitude = randomLat,
                    longitude = randomLng
                )
            )
        }
        list
    }
}
