# Delayed Messaging - Android Client

Modern Android application implementing delayed message delivery with real-time status tracking and offline support.

## Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Kotlin 1.8.x
- Android SDK 33 (API 33)
- Gradle 8.0+
- Firebase project with FCM configured
- API keys for development and production environments

## Project Setup

1. Clone the repository
2. Open project in Android Studio
3. Create `local.properties` file in project root with:
   ```properties
   sdk.dir=<path_to_android_sdk>
   api.key.debug=<debug_api_key>
   api.key.release=<release_api_key>
   fcm.sender.id=<fcm_sender_id>
   ```
4. Sync project with Gradle files
5. Run `./gradlew build` to verify setup

## Architecture Overview

### Core Architecture
- MVVM architecture with Clean Architecture principles
- Dependency injection using Hilt
- Repository pattern for data management
- Use cases for business logic
- Kotlin Coroutines for asynchronous operations
- Flow for reactive programming

### Data Layer
- Room database for offline support
- Repository implementations
- Data source abstractions
- Model mappers
- API service interfaces

### Domain Layer
- Business logic use cases
- Domain models
- Repository interfaces
- Custom exceptions
- Business rules validation

### Presentation Layer
- ViewModels
- UI state management
- Navigation components
- Custom views
- Data binding implementations

## Key Features

### Message Management
- Compose and send delayed messages
- Real-time delivery status tracking
- Message history and search
- Offline message queue
- Retry mechanism for failed messages

### Real-time Communication
- WebSocket implementation for live updates
- Connection state management
- Automatic reconnection handling
- Event-based updates

### Security Implementation

#### API Security
- Certificate pinning configuration
- API key management
- Request signing
- Token-based authentication

#### Data Security
- Encrypted shared preferences
- Secure file storage
- In-memory data protection
- Database encryption

#### Code Security
- ProGuard rules for:
  - Code obfuscation
  - Resource shrinking
  - Library optimization
  - Vulnerability protection

## Testing Guidelines

### Unit Testing
- JUnit 5 for unit tests
- MockK for mocking
- Test coverage requirements
- Repository testing
- ViewModel testing
- Use case testing

### UI Testing
- Espresso for UI tests
- Screen navigation testing
- Input validation testing
- Error handling testing
- Accessibility testing

### Integration Testing
- Hilt testing configuration
- End-to-end testing
- API integration testing
- Database integration testing

## Performance Optimization

### Memory Management
- Image loading optimization
- Cache management
- Memory leak prevention
- Background task handling

### Network Optimization
- Request batching
- Response caching
- Bandwidth optimization
- Offline support

## Build Variants

### Debug Build
- Logging enabled
- Debug API endpoints
- Development environment
- Test coverage reporting

### Release Build
- ProGuard enabled
- Production API endpoints
- Crash reporting
- Analytics enabled

## Dependencies

### Core Dependencies
```groovy
// Kotlin - v1.8.x
implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.8.20'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'

// AndroidX - Latest stable versions
implementation 'androidx.core:core-ktx:1.10.1'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.1'

// Architecture Components
implementation 'androidx.room:room-runtime:2.5.2'
implementation 'androidx.room:room-ktx:2.5.2'
implementation 'androidx.navigation:navigation-fragment-ktx:2.5.3'
implementation 'androidx.navigation:navigation-ui-ktx:2.5.3'

// Dependency Injection
implementation 'com.google.dagger:hilt-android:2.44'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
implementation 'com.squareup.moshi:moshi-kotlin:1.14.0'

// Firebase
implementation platform('com.google.firebase:firebase-bom:32.2.0')
implementation 'com.google.firebase:firebase-messaging-ktx'

// Testing
testImplementation 'org.junit.jupiter:junit-jupiter:5.9.3'
testImplementation 'io.mockk:mockk:1.13.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

## Contribution Guidelines

1. Fork the repository
2. Create feature branch
3. Follow coding standards
4. Write tests
5. Submit pull request

## License

Copyright (c) 2024 Delayed Messaging System

Licensed under the MIT License. See LICENSE file for details.

## Support

For technical support:
- Create GitHub issue
- Contact development team
- Check documentation wiki

## Version History

- 1.0.0 (2024-02-20)
  - Initial release
  - Core messaging functionality
  - Real-time status updates
  - Offline support