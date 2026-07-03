# 🩸 BloodBank App

A modern Android application that connects blood donors with recipients in real time. Built with Kotlin, Firebase, and Material 3 design to help save lives across the Philippines.

---

## 📱 Features

### 🏠 Home — Live Blood Request Map
- Interactive map powered by **Leaflet.js** with **Esri World TopoMap** tiles
- 3 map modes: **Standard**, **Satellite**, and **Dark** — switchable with a glassmorphism floating button
- Real-time blood request pins (🔴 Critical, 🟠 Urgent, ⚪ Normal)
- Tap any pin to see a popup with requester info, blood type, and location
- Toggle between **Map View** and **List View**
- Compact floating stats bar (Critical / Urgent / Total / My Type counts)

### 🏥 Find a Hospital
- Displays **100+ hospitals and blood centers** across the Philippines as blue pins on the map
- Coverage: NCR, Luzon, Visayas, and Mindanao
- Tap a hospital pin to see its name and address, then open **Google Maps** for directions

### 🩸 Become a Donor
- Register your blood type, gender, date of birth, and weight
- Set your location using GPS or manual address input (Province → City → Barangay → Street)
- Modern form with hero banner and grouped Material Cards

### 📋 Blood Request Feed
- Create and manage blood requests
- Filter by: All, Critical, Urgent, My Blood Type
- Each card shows urgency badge, blood type, hospital, and respond button

### ✅ Donation Eligibility (FAQ)
- Scrollable FAQ with Material Cards covering:
  - Age & Weight requirements
  - General Health
  - Tattoos & Piercings
  - Donation Frequency
  - Pregnancy & Women
  - Food & Hydration tips

### 👤 Profile & Settings
- View and edit your profile (name, email, photo)
- Track your donation history
- App settings (notifications, preferences)

### ☰ More Menu
- Profile header with avatar, name, and email
- Quick links: Find a Hospital, Eligibility Checklist, Badges, Settings, Logout

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Material Design 3, ViewBinding |
| Architecture | MVVM + Clean Architecture |
| Navigation | Jetpack Navigation Component |
| DI | Hilt |
| Backend | Firebase Auth, Firestore, Realtime DB, Storage |
| Map | Leaflet.js (WebView) + Esri / CARTO tiles |
| Image Loading | Glide |
| Async | Kotlin Coroutines + Flow |
| Location | Google Play Services Location |
| Address API | PSGC API (Retrofit) |
| Local Cache | Room Database |
| Preferences | DataStore |
| Animations | Lottie |

---

## 🏗 Project Structure

```
app/
├── data/
│   ├── remote/dto/          # Firestore DTOs
│   └── repository/          # Repository implementations
├── domain/
│   ├── model/               # Domain models (BloodRequest, HospitalMarker, etc.)
│   └── repository/          # Repository interfaces
├── presentation/
│   ├── auth/                # Login & Registration screens
│   ├── home/                # Home map, feed, filters
│   ├── more/                # More menu, Settings, Eligibility
│   ├── profile/             # Profile, Become a Donor
│   ├── request/             # Create & Edit blood request
│   ├── respond/             # Respond to a request
│   └── main/                # MainActivity & navigation
└── assets/
    └── map.html             # Leaflet.js map (WebView)
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35 (compileSdk), min SDK 26 (Android 8.0+)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/krukru741/BloodBank.git
   cd BloodBank
   ```

2. **Firebase Configuration**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a project and add an Android app with package `com.example.blood`
   - Download `google-services.json` and place it in `/app/`

3. **Run the app**
   ```bash
   ./gradlew assembleDebug
   ```
   Or simply open the project in Android Studio and click **Run ▶**.

---

## 📸 Screenshots

> *(Coming soon — add screenshots here after building)*

---

## 🔐 Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS for donor/request location |
| `INTERNET` | Firebase, map tiles, PSGC API |
| `POST_NOTIFICATIONS` | Blood request alerts |
| `READ_MEDIA_IMAGES` | Profile photo upload |

---

## 📄 License

This project is for educational and humanitarian purposes.

---

## 👨‍💻 Author

**Alben Gacayan**
- GitHub: [@krukru741](https://github.com/krukru741)
- Email: albengacayan486@gmail.com
