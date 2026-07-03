package com.example.bloodbank.domain.model

data class HospitalMarker(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val contactNumber: String? = null,
    val imageUrl: String? = null
)

object MockHospitalData {
    val hospitals: List<HospitalMarker> = listOf(
        // ── NCR ──────────────────────────────────────────────────────────
        HospitalMarker("h1",  "Philippine Red Cross - National Blood Center",  "Port Area, Manila",            14.5891, 120.9760),
        HospitalMarker("h2",  "Philippine General Hospital",                   "Taft Ave, Ermita, Manila",     14.5794, 120.9841,
            contactNumber = "(02) 8554 8400",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cf/Philippine_General_Hospital_%28PGH%29_Administration_Building_facade.jpg/800px-Philippine_General_Hospital_%28PGH%29_Administration_Building_facade.jpg"
        ),
        HospitalMarker("h3",  "Lung Center of the Philippines",                "Quezon Ave, Quezon City",      14.6471, 121.0470),
        HospitalMarker("h4",  "St. Luke's Medical Center QC",                  "E Rodriguez Sr Ave, QC",       14.6234, 121.0247),
        HospitalMarker("h5",  "Quirino Memorial Medical Center",               "Project 4, Quezon City",       14.6390, 121.0744),
        HospitalMarker("h6",  "The Medical City",                              "Ortigas Ave, Pasig City",      14.5877, 121.0703),
        HospitalMarker("h7",  "Jose R. Reyes Memorial Medical Center",         "Rizal Ave, Sta Cruz, Manila",  14.6078, 120.9876),
        HospitalMarker("h8",  "Manila Doctors Hospital",                       "United Nations Ave, Manila",   14.5834, 120.9813),
        HospitalMarker("h9",  "Las Piñas General Hospital",                    "Pamana St, Las Piñas City",    14.4388, 120.9958),
        HospitalMarker("h10", "Muntinlupa City General Hospital",              "Alabang, Muntinlupa City",     14.4095, 121.0516),
        HospitalMarker("h11", "Valenzuela City Medical Center",                "Karuhatan, Valenzuela City",   14.7041, 120.9742),
        HospitalMarker("h12", "Marikina Valley Medical Center",                "J.P. Rizal St, Marikina",      14.6515, 121.1049),
        HospitalMarker("h13", "Pasig City General Hospital",                   "Caruncho Ave, Pasig City",     14.5766, 121.0891),
        HospitalMarker("h14", "Taguig City Hospital",                          "Western Bicutan, Taguig",      14.5207, 121.0648),

        // ── Luzon — Central Luzon ─────────────────────────────────────────
        HospitalMarker("h15", "Jose B. Lingad Memorial Regional Hospital",     "San Fernando, Pampanga",       15.0357, 120.6963),
        HospitalMarker("h16", "Bulacan Medical Center",                        "Malolos, Bulacan",             14.8439, 120.8145),
        HospitalMarker("h17", "Bataan Provincial Hospital",                    "Balanga, Bataan",              14.6743, 120.5388),
        HospitalMarker("h18", "Olongapo City General Hospital",                "Olongapo City, Zambales",      14.8385, 120.2878),

        // ── Luzon — Central Luzon ────────────────────────────────────────
        HospitalMarker("h19", "Tarlac Provincial Hospital",                    "Tarlac City",                  15.475018, 120.586573),
        HospitalMarker("h20", "Cabanatuan City Medical Center",                "Cabanatuan, Nueva Ecija",      15.476226, 120.957922),
        HospitalMarker("h21", "Angeles University Foundation Medical Center",  "Angeles, Pampanga",            15.145193, 120.595076),

        // ── Luzon — Metro North ──────────────────────────────────────────
        HospitalMarker("h22", "Cagayan Valley Medical Center",                 "Tuguegarao, Cagayan",          17.656475, 121.747247),
        HospitalMarker("h23", "Mariano Marcos Memorial Hospital",              "Batac, Ilocos Norte",          18.060406, 120.559142),
        HospitalMarker("h24", "Ilocos Training & Regional Medical Center",     "San Fernando, La Union",       16.591232, 120.317820),
        HospitalMarker("h25", "Laoag City General Hospital",                   "Laoag, Ilocos Norte",          18.188278, 120.577352),
        HospitalMarker("h26", "Gabriela Silang Memorial Hospital",             "Vigan, Ilocos Sur",            17.553503, 120.379741),

        // ── Luzon — Cordillera (CAR) ─────────────────────────────────────
        HospitalMarker("h27", "Baguio General Hospital & Medical Center",      "Gov. Pack Rd, Baguio City",    16.401071, 120.595672),
        HospitalMarker("h28", "Benguet General Hospital",                      "La Trinidad, Benguet",         16.450765, 120.589133),

        // ── Luzon — Bicol ────────────────────────────────────────────────
        HospitalMarker("h29", "Bicol Medical Center",                          "Panganiban Dr, Naga City",     13.623065, 123.198620),
        HospitalMarker("h30", "Bicol Regional Teaching & Training Hospital",   "Legazpi City, Albay",          13.147007, 123.724704),
        HospitalMarker("h31", "Camarines Sur Provincial Hospital",             "Pili, Camarines Sur",          13.517239, 123.303134),
        HospitalMarker("h32", "Sorsogon Provincial Hospital",                  "Sorsogon City",                12.983231, 123.990004),

        // ── Luzon — Southern Tagalog ─────────────────────────────────────
        HospitalMarker("h33", "Batangas Regional Hospital",                    "Batangas City",                13.766495, 121.066264),
        HospitalMarker("h34", "Lipa City General Hospital",                    "Lipa, Batangas",               13.933697, 121.158577),
        HospitalMarker("h35", "San Pablo City General Hospital",               "San Pablo, Laguna",            14.062811, 121.343255),
        HospitalMarker("h36", "Quezon Medical Center",                         "Lucena City",                  13.942185, 121.612468),
        HospitalMarker("h37", "Ospital ng Palawan",                            "Puerto Princesa City",          9.747837, 118.744252),
        HospitalMarker("h38", "Marinduque Provincial Hospital",                "Boac, Marinduque",              13.4487, 121.8361),
        HospitalMarker("h39", "Romblon Provincial Hospital",                   "Romblon, Romblon",             12.5766, 122.2695),

        // ── Luzon — Rizal & Calabarzon ──────────────────────────────────
        HospitalMarker("h40", "Antipolo City Hospital",                        "Antipolo, Rizal",              14.629251, 121.124034),
        HospitalMarker("h41", "Calamba Medical Center",                        "Calamba, Laguna",              14.206167, 121.152388),

        // ── Visayas — Cebu ───────────────────────────────────────────────
        HospitalMarker("h42", "Vicente Sotto Memorial Medical Center",         "B. Rodriguez St, Cebu City",   10.308007, 123.891592),
        HospitalMarker("h43", "Cebu City Medical Center",                      "Natalio Bacalso Ave, Cebu",    10.297508, 123.891617),
        HospitalMarker("h44", "Chong Hua Hospital",                            "Don Mariano Cui St, Cebu",     10.309829, 123.890954),
        HospitalMarker("h45", "Perpetual Succour Hospital",                    "Gorordo Ave, Cebu City",       10.315116, 123.899820),
        HospitalMarker("h46", "Mandaue City Hospital",                         "Mandaue City, Cebu",           10.323329, 123.942686),
        HospitalMarker("h47", "Lapu-Lapu City General Hospital",               "Lapu-Lapu City, Cebu",         10.301626, 123.950806),
        HospitalMarker("h48", "Bohol Medical Center",                          "Tagbilaran City, Bohol",        9.644209, 123.854483),
        HospitalMarker("h49", "Gov. Celestino Gallares Memorial Hospital",     "Tagbilaran, Bohol",             9.644209, 123.854483),

        // ── Visayas — Western Visayas ────────────────────────────────────
        HospitalMarker("h50", "Western Visayas Medical Center",                "Mandurriao, Iloilo City",      10.718759, 122.541596),
        HospitalMarker("h51", "St. Paul's Hospital Iloilo",                    "General Luna St, Iloilo",      10.701885, 122.566809),
        HospitalMarker("h52", "Corazon Locsin Montelibano Regional Hospital",  "Lacson St, Bacolod City",      10.672217, 122.950994),
        HospitalMarker("h53", "Our Lady of Mercy Hospital",                    "Cadiz City, Negros Occidental",10.9545, 123.3097),
        HospitalMarker("h54", "Negros Occidental Provincial Hospital",         "Silay, Negros Occidental",     10.801290, 122.971060),
        HospitalMarker("h55", "Aklan Provincial Hospital",                     "Kalibo, Aklan",                11.705201, 122.365712),
        HospitalMarker("h56", "Antique Provincial Hospital",                   "San Jose, Antique",            10.755042, 121.932716),
        HospitalMarker("h57", "Capiz Emmanuel Hospital",                       "Roxas City, Capiz",            11.575971, 122.751875),
        HospitalMarker("h58", "Guimaras Provincial Hospital",                  "Jordan, Guimaras",             10.592940, 122.605773),

        // ── Visayas — Eastern Visayas ────────────────────────────────────
        HospitalMarker("h59", "Eastern Visayas Regional Medical Center",       "Tacloban City, Leyte",         11.288195, 124.958272),
        HospitalMarker("h60", "Bethany Hospital",                              "Tacloban City, Leyte",         11.232455, 125.002785),
        HospitalMarker("h61", "Southern Leyte Provincial Hospital",            "Maasin City, Southern Leyte",  10.135068, 124.843612),
        HospitalMarker("h62", "Ormoc City General Hospital",                   "Ormoc City, Leyte",            11.022756, 124.603227),
        HospitalMarker("h63", "Samar Provincial Hospital",                     "Catbalogan, Samar",            11.773834, 124.887254),
        HospitalMarker("h64", "Northern Samar Provincial Hospital",            "Catarman, Northern Samar",     12.454372, 124.644826),

        // ── Visayas — Negros Oriental ────────────────────────────────────
        HospitalMarker("h65", "Silliman University Medical Center",            "Dumaguete City",                9.316436, 123.304213),
        HospitalMarker("h66", "Governor Celestino Gallares Mem. Hospital",     "Guihulngan, Negros Oriental",   10.116340, 123.279831),

        // ── Mindanao — Davao Region ──────────────────────────────────────
        HospitalMarker("h67", "Southern Philippines Medical Center",           "J.P. Laurel Ave, Davao City",   7.098373, 125.619837),
        HospitalMarker("h68", "Davao Doctors Hospital",                        "E. Quirino Ave, Davao City",    7.070285, 125.604717),
        HospitalMarker("h69", "San Pedro Hospital of Davao City",              "Pedro Colis St, Davao",         7.068531, 125.609538),
        HospitalMarker("h70", "Davao Regional Medical Center",                 "Tagum City, Davao del Norte",   7.422297, 125.828714),
        HospitalMarker("h71", "Digos City Hospital",                           "Digos City, Davao del Sur",     6.739834, 125.357712),
        HospitalMarker("h72", "Mati Community Hospital",                       "Mati, Davao Oriental",          6.964829, 126.218483),

        // ── Mindanao — Northern Mindanao ─────────────────────────────────
        HospitalMarker("h73", "Northern Mindanao Medical Center",              "Capitol Compound, CDO",          8.485902, 124.649958),
        HospitalMarker("h74", "Polymedic General Hospital",                    "Cagayan de Oro City",            8.484516, 124.648473),
        HospitalMarker("h75", "Iligan City Medical Center",                    "Iligan City, Lanao del Norte",   8.229714, 124.244582),
        HospitalMarker("h76", "Mayor Hilarion A. Ramiro Sr. Regional Hospital","Ozamiz City, Mis. Occidental",  8.148320, 123.845716),
        HospitalMarker("h77", "Camiguin General Hospital",                     "Mambajao, Camiguin",             9.257847, 124.724159),
        HospitalMarker("h78", "Misamis Oriental Provincial Hospital",          "Gingoog City",                   8.826420, 125.110421),

        // ── Mindanao — Zamboanga ─────────────────────────────────────────
        HospitalMarker("h79", "Zamboanga City Medical Center",                 "Dr. D. Evangelista St, Zamboanga",6.907251, 122.080982),
        HospitalMarker("h80", "Labuan Public Hospital",                        "Isabela City, Basilan",          6.706814, 121.970897),
        HospitalMarker("h81", "Pagadian City Hospital",                        "Pagadian City, Zamboanga del Sur",7.831124, 123.434879),
        HospitalMarker("h82", "Dipolog City Hospital",                         "Dipolog City, Zamboanga del Norte",8.585943, 123.341348),

        // ── Mindanao — SOCCSKSARGEN ──────────────────────────────────────
        HospitalMarker("h83", "General Santos City General Hospital",          "General Santos City",            6.125741, 125.185649),
        HospitalMarker("h84", "Cotabato Regional Medical Center",              "Cotabato City",                  7.200236, 124.236290),
        HospitalMarker("h85", "Kidapawan City General Hospital",               "Kidapawan, North Cotabato",      7.003842, 124.618263),
        HospitalMarker("h86", "Koronadal City Medical Center",                 "Koronadal, South Cotabato",      6.502134, 124.846739),

        // ── Mindanao — Caraga ────────────────────────────────────────────
        HospitalMarker("h87", "Caraga Regional Hospital",                      "Surigao City, Surigao del Norte", 9.784373, 125.490032),
        HospitalMarker("h87b", "Democrito O. Plaza Memorial Hospital (DOPMH)", "Patin-ay, Prosperidad, Agusan del Sur", 8.546700, 125.942360,
            contactNumber = "0920-845-6925",
            imageUrl = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?q=80&w=600&auto=format&fit=crop" // Generic modern hospital fallback
        ),
        HospitalMarker("h88", "Butuan City Hospital",                          "Butuan City, Agusan del Norte",  8.944871, 125.543694),
        HospitalMarker("h89", "Tandag City General Hospital",                  "Tandag, Surigao del Sur",        9.068550, 126.190386),
        HospitalMarker("h90", "Bislig City General Hospital",                  "Bislig, Surigao del Sur",        8.215736, 126.319854),

        // ── Mindanao — BARMM ─────────────────────────────────────────────
        HospitalMarker("h91", "Amai Pakpak Medical Center",                    "Marawi City, Lanao del Sur",     7.998467, 124.295028),
        HospitalMarker("h92", "Sulu Provincial Hospital",                      "Jolo, Sulu",                     6.051347, 121.000283),

        // ── Luzon — more NCR area ────────────────────────────────────────
        HospitalMarker("h93", "Caloocan City Medical Center",                  "Caloocan City",                 14.651428, 120.966847),
        HospitalMarker("h94", "Rizal Medical Center",                          "Pasig City, Metro Manila",      14.568329, 121.074836),
        HospitalMarker("h95", "San Lazaro Hospital",                           "Sta Cruz, Manila",              14.604381, 120.988562),

        // ── Luzon — Laguna / Cavite ──────────────────────────────────────
        HospitalMarker("h96", "De La Salle University Medical Center",         "Dasmariñas, Cavite",            14.327316, 120.942804),
        HospitalMarker("h97", "Ospital ng Cavite",                             "Trece Martires, Cavite",        14.284513, 120.867543),
        HospitalMarker("h98", "Laguna Provincial Hospital",                    "Sta Cruz, Laguna",              14.274219, 121.416372),

        // ── Visayas — Siquijor / Biliran ─────────────────────────────────
        HospitalMarker("h99",  "Siquijor Provincial Hospital",                 "Siquijor, Siquijor",             9.206512, 123.527183),
        HospitalMarker("h100", "Biliran Provincial Hospital",                  "Naval, Biliran",                11.568943, 124.402758)
    )
}
