# 📅 Attendance Tracker

A modern, feature-packed Android application built with **Kotlin**, **Jetpack Compose**, and **Room Database** to help students manage their weekly class timetables, track attendance targets, and automate daily attendance logging.

---

## ✨ Key Features

- **🗓️ Attendance Calendar & Monthly Timeline**: Full monthly calendar view with color-coded day attendance status indicators (🟢 Attended, 🔴 Missed, ⚪ Cancelled) and class-by-class inspection.
- **🔮 What-If Attendance Simulator**: Interactive steppers to test future attendance scenarios and preview projected percentages and safe-to-skip margins in real time.
- **🔔 Daily Attendance Reminders**: Background reminders scheduled via Android WorkManager to remind you to log classes before the 5:00 PM cutoff.
- **🧭 Interactive In-App Tour**: First-time user onboarding overlay guiding new users through timetable setup, class substitutions, and simulators.
- **🎓 Class Start Date Control**: Configure the exact day when your classes start. Attendance logging and automated 5:00 PM confirmation rules only kick in from your start day onwards.
- **🗓️ Dynamic 7-Day Slider**: Home screen displays a Mon–Sun week slider starting strictly from the week of your start date.
- **⚡ Automated & Manual Attendance Logging**:
  - **5:00 PM Auto-Confirm**: Automatically marks pending classes for the current day as *Attended* after 5:00 PM.
  - **Missed / Attended Toggle**: Easily override any class status with one tap.
  - **Reassign Class Slot**: Mark a class slot as taken by another subject (*Taken by Another Subject*) when proxy/extra lectures occur.
- **📚 Timetable Management & JSON Import/Export**:
  - Add and edit subjects with custom color pickers and target percentages.
  - Full **AI-compatible JSON import and export** for easy schedule sharing and backup.
- **📊 Subject Analytics & Target Calculator**:
  - Real-time attendance percentage tracking for every subject.
  - Calculates exactly how many consecutive classes you must attend (or can safely skip) to maintain your target attendance percentage.
- **🎨 Modern Design & Themes**:
  - Clean Material 3 UI with glassmorphism touches and smooth Compose animations.
  - Multi-theme support: **Light**, **Dark**, and **AMOLED (Pure Black)**.
- **🔒 100% Offline & Private**: All data is stored locally on your device via Room SQLite.

---

## 🛠️ Architecture & Tech Stack

- **UI**: Jetpack Compose, Material 3, Custom Canvas Drawing
- **Architecture**: MVVM (Model-View-ViewModel), StateFlow, ViewModelScope
- **Database & Storage**: Room Database, SharedPreferences
- **Language**: Kotlin 2.0+ (JVM Toolchain 17)
- **Min SDK**: Android 7.0 (API Level 24)
- **Target SDK**: API Level 36

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug / Jellyfish or newer recommended)
- **JDK 17** installed and configured
- Android device or emulator running **Android 7.0+**

### Building the App

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/attendance-app.git
   cd attendance-app
   ```

2. **Compile and Build Debug APK**:
   ```bash
   # On Windows PowerShell
   .\gradlew.bat assembleDebug

   # On Linux / macOS
   ./gradlew assembleDebug
   ```

3. **Output Location**:
   The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Screenshots & Overview

| Home Screen | Timetable Schedule | Subject Stats |
| :---: | :---: | :---: |
| Dynamic Week Slider & Today's Schedule | Weekly Timetable & JSON Import/Export | Target Attendance & Safety Calculations |

---

## 📄 License

This project is licensed under the MIT License.
