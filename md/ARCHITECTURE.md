# Socially - Android App with MVVM Architecture

## Project Structure

The project follows a **feature-based MVVM (Model-View-ViewModel)** architecture for better organization and maintainability.

```
app/src/main/java/com/apollo/socially/
│
├── data/                           # Data Layer
│   └── repository/
│       └── AuthRepositoryImpl.kt   # Firebase Auth & Firestore implementation
│
├── domain/                         # Domain Layer
│   ├── model/
│   │   └── User.kt                 # User data model
│   └── repository/
│       └── IAuthRepository.kt      # Repository interface
│
├── ui/                             # Presentation Layer (organized by feature)
│   └── auth/                       # Authentication Feature
│       ├── login/
│       │   ├── LoginActivity.kt    # Login UI
│       │   └── LoginViewModel.kt   # Login business logic & state
│       └── register/
│           ├── RegisterActivity.kt # Register UI
│           └── RegisterViewModel.kt # Register business logic & state
│
├── utils/                          # Utility classes
│   └── ViewModelFactory.kt         # ViewModel factory for dependency injection
│
└── MainActivity.kt                 # Main app activity (post-login)
```

## MVVM Architecture Layers

### 1. **Data Layer** (`data/`)
- **Purpose:** Handles data operations and external data sources
- **Components:**
  - `AuthRepositoryImpl.kt`: Implements authentication logic using Firebase Auth and Firestore
  - Future: Can include local database (Room), network APIs, etc.

### 2. **Domain Layer** (`domain/`)
- **Purpose:** Contains business models and repository interfaces
- **Components:**
  - `model/User.kt`: Data class representing a user
  - `repository/IAuthRepository.kt`: Interface defining authentication operations
- **Benefits:** Decouples business logic from implementation details

### 3. **Presentation Layer** (`ui/`)
- **Purpose:** Handles UI and user interactions
- **Organization:** By feature (login, register, profile, posts, etc.)
- **Components per feature:**
  - **Activity/Fragment:** View that displays UI
  - **ViewModel:** Manages UI state and business logic
  - **State classes:** Sealed classes representing different UI states

## Feature: Authentication

### Login Feature (`ui/auth/login/`)

**LoginActivity.kt**
- Displays login UI
- Observes `LoginViewModel` state
- Handles Google Sign-In
- Navigates to MainActivity on success

**LoginViewModel.kt**
- Manages login state (Idle, Loading, Success, Error)
- Validates user input
- Communicates with `IAuthRepository`
- Exposes StateFlow for UI observation

**States:**
```kotlin
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val uid: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

### Register Feature (`ui/auth/register/`)

**RegisterActivity.kt**
- Displays registration UI
- Observes `RegisterViewModel` state
- Validates password confirmation
- Navigates to MainActivity on success

**RegisterViewModel.kt**
- Manages registration state
- Validates input (name, email, password match)
- Creates user in Firebase Auth & Firestore
- Exposes StateFlow for UI observation

## Firebase Integration

### Authentication
- **Email/Password:** Users can register and login with email
- **Google Sign-In:** OAuth integration for Google accounts
- **Apple Sign-In:** Coming soon

### Firestore Database
- **Collection:** `users`
- **Document ID:** User's Firebase Auth UID
- **Fields:**
  - `uid`: String
  - `email`: String
  - `fullName`: String
  - `profileImageUrl`: String? (optional)
  - `createdAt`: Timestamp
  - `updatedAt`: Timestamp

### Security Rules
See `FIRESTORE_RULES.md` for detailed documentation on Firestore security rules.

## Key Benefits of This Architecture

### ✅ Separation of Concerns
- UI logic separated from business logic
- Data operations isolated in repositories
- Easy to test each layer independently

### ✅ Feature-Based Organization
- All related code for a feature is in one place
- Easy to find and modify feature-specific code
- Scalable as app grows

### ✅ Reactive UI Updates
- ViewModels expose StateFlow
- UI automatically updates when state changes
- No manual UI updates needed

### ✅ Testability
- ViewModels can be unit tested without Android framework
- Repository interface allows easy mocking
- Business logic is decoupled from UI

### ✅ Maintainability
- Clear structure makes onboarding easier
- Changes are localized to specific features
- Reduces merge conflicts in team development

## Adding New Features

To add a new feature (e.g., "Posts"), follow this structure:

```
ui/posts/
├── PostsActivity.kt or PostsFragment.kt
├── PostsViewModel.kt
├── CreatePostActivity.kt
├── CreatePostViewModel.kt
└── PostDetailActivity.kt
    └── PostDetailViewModel.kt
```

1. Create feature folder in `ui/`
2. Add Activity/Fragment for UI
3. Add ViewModel for business logic
4. Define state sealed class
5. Update repository if needed

## Dependencies

- **Firebase Auth:** User authentication
- **Firebase Firestore:** NoSQL cloud database
- **Kotlin Coroutines:** Asynchronous programming
- **StateFlow:** Reactive state management
- **ViewModel & LiveData:** Android Architecture Components
- **Google Sign-In:** OAuth authentication

## Getting Started

1. Clone the repository
2. Add your `google-services.json` to `app/` directory
3. Update `strings.xml` with your `default_web_client_id`
4. Deploy Firestore rules from `firestore.rules`
5. Sync and build the project

## Future Enhancements

- [ ] Forgot Password functionality
- [ ] Apple Sign-In integration
- [ ] Profile editing
- [ ] Posts feature
- [ ] Comments feature
- [ ] Real-time updates
- [ ] Image upload
- [ ] Push notifications

## Contributing

When adding new features:
1. Follow the feature-based structure
2. Use MVVM pattern
3. Create ViewModels for business logic
4. Use StateFlow for state management
5. Document your code
6. Update this README

## License

[Your License Here]

