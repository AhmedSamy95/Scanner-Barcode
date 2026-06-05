# 📱 ScanPro — The Ultimate Warehouse Scanning Companion

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue)](#)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**ScanPro** isn't just another barcode reader. It is a tailored, high-speed corporate utility built specifically for logistics heroes, warehouse operations coordinators, and supply chain analysts. It transforms any standard Android device into an industrial-grade scanning powerhouse, making physical auditing, inventory tracking, and data collection smoother and faster than ever before.

---

## 📸 App Preview & Interface

| Modern Scan Dashboard | Smart History & Analytics |
|:---:|:---:|
| <img src="screenshots/scan_screen.png" width="280" alt="Scan Dashboard"/> | <img src="screenshots/history_screen.png" width="280" alt="History Screen"/> |

*Note: Replace the placeholder paths above with your actual screenshot images inside a `screenshots/` folder.*

---

## ✨ Supercharged Features

* ⚡ **Lightning-Fast Engine:** Powered by Google's advanced ML Kit, capturing barcodes (QR, Data Matrix, Code 128, and more) instantly—even under tough warehouse lighting.
* 🔄 **Continuous Scanning Mode:** Non-stop scanning workflow. Scan item after item consecutively without annoying pop-ups or interruption.
* ⭐ **Smart History & Favorites:** Never lose a package ID. Automatically logs your scans, allowing you to instantly filter, sort by preference, and bookmark crucial barcodes.
* 📊 **One-Click CSV/Excel Export:** Instantly bridge the gap between physical floor-work and digital reporting. Export clean, structured data sheets directly to your device storage.
* 🌙 **Dark Mode Ready:** Designed with a sleek, eye-friendly dark theme optimized for long night shifts in high-volume environments.

---

## 🏗️ Cutting-Edge Architecture & Tech Stack

ScanPro is engineered following the strict, modern Android development guidelines to ensure rock-solid stability and speed:

* **Architecture:** Clean **MVVM (Model-View-ViewModel)** for distinct separation of concerns and robust data flow.
* **Local Database:** **Jetpack Room**—providing a lightning-fast, secure local SQLite cache for your historical logs.
* **Reactive Async Pipeline:** Driven by **Kotlin Coroutines & Flow** to handle heavy background processing without a single UI stutter.
* **Core scanning Engine:** Google ML Kit Barcode Scanning API.

---

## 🚀 Installation & Quick Start

Get your development environment running in less than 2 minutes:

1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/YOUR_USERNAME/ScanPro.git](https://github.com/YOUR_USERNAME/ScanPro.git)