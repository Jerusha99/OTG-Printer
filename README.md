# OTG-Printer 🖨️

An Android application for printing documents via USB OTG (On-The-Go) connection. This app enables direct printing to USB-connected printers from your Android device without requiring a wireless network.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)

## 📋 Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Supported Printers](#supported-printers)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)
- [Author](#author)
- [Acknowledgments](#acknowledgments)

## ✨ Features

- **Direct USB Connection**: Print directly via USB OTG cable without WiFi or Bluetooth
- **Multiple Document Formats**: Support for PDF, images, and text files
- **Printer Detection**: Automatic detection of connected USB printers
- **Print Preview**: Preview documents before printing
- **Custom Settings**: Adjust paper size, orientation, and print quality
- **Print Queue Management**: Manage multiple print jobs
- **Battery Efficient**: Minimal battery consumption during printing operations
- **No Internet Required**: Works completely offline

## 📱 Requirements

### Hardware Requirements
- Android device with USB OTG support
- USB OTG cable or adapter
- Compatible USB printer (see [Supported Printers](#supported-printers))

### Software Requirements
- Android 5.0 (Lollipop) or higher
- Minimum 2GB RAM recommended
- USB Host API support

## 🚀 Installation

### From Source

1. Clone the repository:
```bash
git clone https://github.com/Jerusha99/OTG-Printer.git
cd OTG-Printer
```

2. Open the project in Android Studio:
```bash
# Open Android Studio and select "Open an Existing Project"
# Navigate to the cloned directory
```

3. Build the project:
```bash
# In Android Studio: Build > Make Project
# Or use Gradle command:
./gradlew assembleDebug
```

4. Install on your device:
```bash
# Connect your Android device via USB
# Enable USB Debugging in Developer Options
./gradlew installDebug
```

### From APK (When Available)

1. Download the latest APK from the [Releases](https://github.com/Jerusha99/OTG-Printer/releases) page
2. Enable "Install from Unknown Sources" in your Android settings
3. Open the APK file and follow the installation prompts

## 📖 Usage

### Basic Printing

1. **Connect your printer**:
   - Connect your USB printer to your Android device using an OTG cable
   - The app will automatically detect the printer

2. **Open a document**:
   - Launch the OTG-Printer app
   - Tap "Select Document" to choose a file to print
   - Supported formats: PDF, JPG, PNG, TXT

3. **Configure print settings**:
   - Select paper size (A4, Letter, etc.)
   - Choose orientation (Portrait/Landscape)
   - Set number of copies
   - Adjust print quality

4. **Print**:
   - Review the print preview
   - Tap "Print" to start printing

### Advanced Features

#### Print Queue
- View all pending print jobs
- Pause, resume, or cancel individual jobs
- Reorder print queue priority

#### Printer Management
- Save printer profiles for different devices
- Configure default settings per printer
- View printer status and ink levels (if supported)

## 🖨️ Supported Printers

The app supports most USB printers that are compatible with Android's USB Host API. Tested printers include:

- **HP**: DeskJet, LaserJet, OfficeJet series
- **Canon**: PIXMA, ImageCLASS series
- **Epson**: EcoTank, WorkForce series
- **Brother**: HL, MFC, DCP series
- **Samsung**: Xpress, ProXpress series

> **Note**: Printer support depends on the manufacturer's implementation of USB standards. Some proprietary features may not be available.

## 🛠️ Technology Stack

- **Language**: Java/Kotlin
- **Build System**: Gradle
- **UI Framework**: Android SDK
- **Architecture**: MVVM (Model-View-ViewModel)
- **USB Communication**: Android USB Host API
- **Document Parsing**: 
  - PDFBox for PDF handling
  - Android Graphics API for images
- **Testing**: JUnit, Espresso

## 📁 Project Structure

```
OTG-Printer/
├── app/                    # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/      # Java/Kotlin source files
│   │   │   ├── res/       # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/          # Unit tests
│   └── build.gradle       # App-level Gradle configuration
├── gradle/                # Gradle wrapper files
├── .gitignore
├── build.gradle          # Project-level Gradle configuration
├── settings.gradle       # Gradle settings
└── README.md            # This file
```

## ⚙️ Configuration

### Permissions

The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.USB_PERMISSION"/>
<uses-feature android:name="android.hardware.usb.host"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### Build Configuration

Minimum SDK: 21 (Android 5.0)
Target SDK: 33 (Android 13)
Compile SDK: 33

## 🔧 Troubleshooting

### Printer Not Detected
- Ensure your device supports USB OTG (check with OTG Checker apps)
- Try a different OTG cable or adapter
- Restart the app after connecting the printer
- Check if your printer is USB-powered or needs external power

### Print Quality Issues
- Update printer firmware
- Clean printer heads
- Check paper quality and type settings
- Adjust print quality settings in the app

### App Crashes
- Ensure you have the latest version installed
- Clear app cache and data
- Check if your device has sufficient free storage
- Report the issue with logs on GitHub Issues

### Permission Denied Errors
- Grant all required permissions in Android Settings
- For Android 11+, enable "All Files Access" if needed
- Reinstall the app if permissions are not properly set

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch:
   ```bash
   git checkout -b feature/YourFeatureName
   ```
3. Commit your changes:
   ```bash
   git commit -m 'Add some feature'
   ```
4. Push to the branch:
   ```bash
   git push origin feature/YourFeatureName
   ```
5. Open a Pull Request

### Development Guidelines

- Follow Android development best practices
- Write unit tests for new features
- Update documentation as needed
- Ensure code is properly formatted
- Test on multiple Android versions if possible

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Jerusha Sharon**
- GitHub: [@Jerusha99](https://github.com/Jerusha99)
- Email: jerushasharon1999@gmail.com

## 🙏 Acknowledgments

- Android USB Host API documentation
- Open source printing libraries community
- All contributors and testers
- USB OTG community for hardware compatibility information

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Troubleshooting](#troubleshooting) section
2. Search existing [Issues](https://github.com/Jerusha99/OTG-Printer/issues)
3. Create a new issue with detailed information:
   - Device model and Android version
   - Printer model
   - Steps to reproduce the issue
   - Screenshots if applicable

## 🗺️ Roadmap

- [ ] Add support for Bluetooth printers
- [ ] Implement cloud printing integration
- [ ] Add more document format support (DOCX, XLSX)
- [ ] Multi-language support
- [ ] Dark mode theme
- [ ] Print history and analytics
- [ ] Batch printing capabilities

---

**Star ⭐ this repository if you find it helpful!**
