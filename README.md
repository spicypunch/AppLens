# AppLens 🔍

**Inspect packaged artifacts from the apps installed on your device**

AppLens is an Android application analyzer focused on verified packaged artifacts. It inspects the APKs that are visible to the current build, reports the framework evidence it can actually see, and lists observed packaged libraries, native shared objects, and manifest permissions without inventing missing data.

## 🚀 Features

### 📱 **Evidence-Based App Analysis**
- **Framework Evidence Detection**: Identifies Flutter, React Native, Xamarin, Cordova, and Unity only when strong packaged evidence is present
- **Observed Library Discovery**: Lists only the packaged libraries and artifacts that are directly observed inside APK files
- **Native Shared Object Inspection**: Aggregates `.so` files across base and split APKs with ABI-aware deduplication

### 🎯 **Smart Filtering & Search**
- **Dual App Categories**: Easily switch between "App Apps" (downloaded applications) and "User Apps" (system applications)
- **Real-time Search**: Instantly find apps with live search functionality
- **Smart Sorting**: Sort applications by name, installation date, or app type

### 📊 **Detailed App Information**
- Application metadata (version, target SDK, packaged APK size)
- Framework evidence and classification summary
- Observed packaged libraries and native shared objects
- Manifest permissions with grant and flag details

### 🎨 **Modern UI/UX**
- Clean, intuitive Material Design 3 interface
- Responsive design optimized for all screen sizes
- Smooth animations and transitions
- Dark/Light theme support following system preferences

## 🆕 Release Notes - v1.0

### ✨ **New Features**
- **Verified Artifact Analysis Engine**: Reports only the framework and library evidence that can be backed by packaged APK contents
- **Split APK Support**: Scans the base APK plus visible split APKs to improve packaged library and native shared object coverage
- **Dual Distribution Modes**:
  - `internal`: full-device visibility for internal and sideloaded builds
  - `play`: Android package visibility-safe mode for public distribution

- **Intelligent App Categorization**: 
  - Automatically separates user-installed apps from system apps
  - Visual indicators for different app types
  - Quick toggle between categories

- **Enhanced Search & Navigation**:
  - Lightning-fast search with real-time results
  - Multiple sorting options for better organization
  - Smooth navigation between app list and detailed views

### 🛠 **Technical Improvements**
- **No Synthetic Results**: The app no longer fills in guessed frameworks or default library lists without evidence
- **Visibility-Aware Results**: Analysis explicitly reflects the package visibility constraints of the current build
- **Modern Architecture**: Built with Jetpack Compose and activity-scoped state management

### 🎨 **Design & User Experience**
- **Material Design 3**: Latest design system implementation
- **Custom App Icon**: Professional, purpose-built icon representing app analysis
- **Responsive Layout**: Optimized for phones and tablets
- **Accessibility**: Full support for screen readers and accessibility services

## 📱 **System Requirements**
- Android 7.0 (API level 24) or higher
- 50MB free storage space
- Internet connection for ads (optional)

## 🔧 **Installation**
1. Choose the appropriate build from the releases section
   - `internal` for maximum app visibility in internal/sideloaded use
   - `play` for package-visibility-safe public distribution
2. Enable "Install from unknown sources" in your device settings
3. Install the APK file
4. Grant necessary permissions when prompted

## 🏗 **Built With**
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - Design system
- **Android Architecture Components** - MVVM pattern implementation
- **Gradle** - Build system

## 🎯 **Target Audience**
- **Android Developers** - Analyze app architectures and learn from other applications
- **Security Researchers** - Investigate app compositions and identify potential security vectors  
- **Tech Enthusiasts** - Explore the technical details of your favorite apps
- **Students** - Learn about different mobile development frameworks in real-world applications

## 🔍 **Use Cases**
- **Framework Research**: Inspect packaged framework evidence in real-world Android apps
- **Security Analysis**: Review packaged libraries and manifest permissions without synthetic guesses
- **Competitive Analysis**: Compare the observable packaged stack of comparable applications
- **Learning Tool**: Study how different app frameworks surface in shipped Android packages
- **App Inventory**: Review visible installed apps under the current distribution mode

## 📈 **Performance**
- **Lazy Analysis**: App details are analyzed on demand and cached in memory for the current session
- **Low Memory Usage**: Packaged artifact scans are scoped to the selected app
- **Offline Capable**: Core functionality works without internet connection

## 🛡 **Privacy & Security**
- **No Data Collection**: AppLens does not collect or transmit personal data
- **Local Analysis**: All artifact inspection runs locally on your device
- **Visibility Respecting**: Public builds clearly communicate Android package visibility limits
- **Safe Scanning**: Uses Android's public APIs and packaged APK contents only

## 🤝 **Contributing**
We welcome contributions! Please feel free to submit issues, feature requests, or pull requests.

## 📄 **License**
This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 **Support**
For support, bug reports, or feature requests, please create an issue in this repository.

---

**AppLens v1.0** - Bringing transparency to your Android app ecosystem 🔍
