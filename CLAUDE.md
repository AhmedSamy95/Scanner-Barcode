# ScanPro — Codebase Reference for AI Code Generation

> Use this document as the authoritative style and architecture guide when generating new code for this project.
> Every new file, class, function, or composable must conform to the patterns documented here.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Dependencies](#2-tech-stack--dependencies)
3. [Architecture](#3-architecture)
4. [Package & Directory Structure](#4-package--directory-structure)
5. [Naming Conventions](#5-naming-conventions)
6. [Data Layer](#6-data-layer)
7. [Domain Layer](#7-domain-layer)
8. [Presentation Layer (UI)](#8-presentation-layer-ui)
9. [Dependency Injection (Hilt)](#9-dependency-injection-hilt)
10. [Navigation](#10-navigation)
11. [OOP Principles & Patterns](#11-oop-principles--patterns)
12. [State Management & Reactive Patterns](#12-state-management--reactive-patterns)
13. [Code Style Rules](#13-code-style-rules)
14. [Feature Addition Checklist](#14-feature-addition-checklist)

---

## 1. Project Overview

**App:** ScanPro — Android barcode scanner and generator  
**Package:** `com.example.scanpro`  
**Min SDK:** 26 | **Target SDK:** 35 | **Compile SDK:** 35  
**Language:** Kotlin 2.1.20 | **JVM:** Java 17  
**Build:** Gradle 9 with KSP 2.1.20-1.0.32, version catalog (`libs.versions.toml`)

---

## 2. Tech Stack & Dependencies

| Category | Library | Version |
|---|---|---|
| UI | Jetpack Compose + Material 3 | BOM managed |
| Navigation | Navigation Compose | 2.8.5 |
| DI | Hilt + Hilt Navigation Compose | 2.55 / 1.2.0 |
| Database | Room | 2.7.0-alpha13 |
| Preferences | DataStore Preferences | 1.1.1 |
| Camera | CameraX (core, camera2, lifecycle, view) | 1.4.1 |
| Barcode Detection | ML Kit Barcode Scanning | 17.3.0 |
| Barcode Generation | ZXing Core | 3.5.3 |
| Serialization | Gson | 2.11.0 |
| Permissions | Accompanist Permissions | 0.36.0 |
| Annotation Processing | KSP | 2.1.20-1.0.32 |
| Testing | JUnit 4 + Coroutines Test + Espresso | standard |

**Rules:**
- Add new dependencies via `gradle/libs.versions.toml` — never hard-code versions in `build.gradle.kts`.
- Use KSP, not KAPT.

---

## 3. Architecture

**Pattern:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)

```
┌──────────────────────────────────────────┐
│           Presentation Layer             │
│  Screen.kt (@Composable)                 │
│  ViewModel.kt (@HiltViewModel)           │
│  UiState (immutable data class)          │
└────────────────┬─────────────────────────┘
                 │ StateFlow<UiState>
                 ▼
┌──────────────────────────────────────────┐
│            Domain Layer                  │
│  UseCase.kt (operator invoke)            │
│  Model.kt (data class, no Android deps)  │
│  Repository.kt (interface)               │
└────────────────┬─────────────────────────┘
                 │ Flow / suspend fun
                 ▼
┌──────────────────────────────────────────┐
│             Data Layer                   │
│  RepositoryImpl.kt                       │
│  Dao.kt (Room)                           │
│  Entity.kt (Room)                        │
│  DataStore (preferences)                 │
└──────────────────────────────────────────┘
```

**Mandatory rules:**
- Domain layer has **zero** Android framework imports — pure Kotlin.
- Data layer owns entities; domain layer owns models. Mappers live in the domain model file.
- UI layer never accesses the data layer directly — always through a UseCase or ViewModel.
- Repositories are injected via their **interface**, never their implementation.

---

## 4. Package & Directory Structure

```
app/src/main/java/com/example/scanpro/
├── data/
│   ├── export/
│   │   └── ExportManager.kt
│   ├── local/
│   │   ├── datastore/
│   │   │   └── SettingsDataStore.kt
│   │   └── db/
│   │       ├── AppDatabase.kt
│   │       ├── BarcodeDao.kt
│   │       └── BarcodeEntity.kt
│   └── repository/
│       └── BarcodeRepositoryImpl.kt
├── di/
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/
│   │   ├── BarcodeItem.kt          ← domain model + toEntity() / toDomainModel() mappers
│   │   ├── BarcodeFormatType.kt    ← enum
│   │   └── BarcodeContentType.kt  ← enum
│   ├── repository/
│   │   └── BarcodeRepository.kt   ← interface
│   └── usecase/
│       ├── ScanBarcodeUseCase.kt
│       ├── GenerateBarcodeUseCase.kt
│       ├── ExportHistoryUseCase.kt
│       ├── ImportHistoryUseCase.kt
│       └── AnalyzeBarcodeUseCase.kt
├── ui/
│   ├── components/
│   │   ├── BarcodeListItem.kt
│   │   └── FormatChip.kt
│   ├── scanner/
│   │   ├── ScannerScreen.kt
│   │   ├── ScannerViewModel.kt
│   │   ├── BarcodeAnalyzer.kt
│   │   ├── CameraPreview.kt
│   │   └── ViewfinderOverlay.kt
│   ├── generator/
│   │   ├── GeneratorScreen.kt
│   │   └── GeneratorViewModel.kt
│   ├── history/
│   │   ├── HistoryScreen.kt
│   │   └── HistoryViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── detail/
│   │   ├── DetailScreen.kt
│   │   └── DetailViewModel.kt
│   └── main/
│       └── MainScreen.kt
├── theme/
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
├── MainActivity.kt
├── ScanProApp.kt
├── Navigation.kt
└── NavigationKeys.kt
```

**When adding a new feature `Foo`:**
1. Create `ui/foo/FooScreen.kt` + `FooViewModel.kt`
2. Create `domain/usecase/FooUseCase.kt` (if new business logic)
3. Add any new domain models to `domain/model/`
4. Add data layer only if new persistence is needed

---

## 5. Naming Conventions

### Files

| Type | Pattern | Example |
|---|---|---|
| Screen (Composable) | `{Feature}Screen.kt` | `ScannerScreen.kt` |
| ViewModel | `{Feature}ViewModel.kt` | `ScannerViewModel.kt` |
| UI State | `{Feature}UiState` (inside ViewModel file) | `ScannerUiState` |
| Use Case | `{Action}UseCase.kt` | `ScanBarcodeUseCase.kt` |
| Repository Interface | `{Entity}Repository.kt` | `BarcodeRepository.kt` |
| Repository Impl | `{Entity}RepositoryImpl.kt` | `BarcodeRepositoryImpl.kt` |
| DAO | `{Entity}Dao.kt` | `BarcodeDao.kt` |
| Room Entity | `{Entity}Entity.kt` | `BarcodeEntity.kt` |
| Domain Model | `{Entity}.kt` (no suffix) | `BarcodeItem.kt` |
| Enum | `{Noun}Type.kt` | `BarcodeFormatType.kt` |
| DI Module | `{Feature}Module.kt` | `DatabaseModule.kt` |
| Reusable Composable | `{Descriptor}.kt` | `BarcodeListItem.kt`, `FormatChip.kt` |

### Properties & Functions

| Type | Pattern | Example |
|---|---|---|
| Private mutable state | `_camelCase` | `_uiState`, `_searchQuery` |
| Public immutable state | `camelCase` | `uiState`, `feedbackMessage` |
| Event handlers | `on{Action}` | `onBarcodesDetected`, `onInputTextChanged` |
| Setters | `set{Property}` | `setZoomRatio`, `setContinuousScan` |
| Toggles | `toggle{Property}` | `toggleFlash`, `toggleFavorite` |
| Boolean flags in UiState | `is{Adjective}` | `isLoading`, `isCameraActive`, `isFavorite` |
| Enum companions | `from{Source}()` | `fromMlKitFormat()`, `fromRawValue()` |
| Mappers | `to{Target}()` (extension) | `toEntity()`, `toDomainModel()` |

---

## 6. Data Layer

### Room Entity Template

```kotlin
@Entity(tableName = "table_name")
data class FooEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val field1: String,
    val field2: Int,
    val timestamp: Long = System.currentTimeMillis()
)
```

### DAO Template

```kotlin
@Dao
interface FooDao {
    @Query("SELECT * FROM foo ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FooEntity>>

    @Query("SELECT * FROM foo WHERE id = :id")
    suspend fun getById(id: Long): FooEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FooEntity): Long

    @Update
    suspend fun update(entity: FooEntity)

    @Delete
    suspend fun delete(entity: FooEntity)

    @Query("DELETE FROM foo")
    suspend fun deleteAll()
}
```

**Rules:**
- Reactive reads return `Flow<T>` — never suspend for reads.
- Writes are `suspend fun` — never return Flow.
- Always use `OnConflictStrategy.REPLACE` for inserts (upsert behavior).
- Add new entities to `AppDatabase.kt` entities list and bump version.

### Repository Implementation Template

```kotlin
class FooRepositoryImpl @Inject constructor(
    private val fooDao: FooDao
) : FooRepository {

    override fun getAll(): Flow<List<FooItem>> =
        fooDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun insert(item: FooItem) {
        fooDao.insert(item.toEntity())
    }
}
```

### DataStore Preferences Pattern

```kotlin
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.createDataStoreWithDefaults()

    private object Keys {
        val SOME_PREF = booleanPreferencesKey("some_pref")
    }

    val somePref: Flow<Boolean> = dataStore.data.map { it[Keys.SOME_PREF] ?: false }

    suspend fun setSomePref(value: Boolean) {
        dataStore.edit { it[Keys.SOME_PREF] = value }
    }
}
```

---

## 7. Domain Layer

### Domain Model + Mapper Template

```kotlin
// BarcodeItem.kt — all mappers live here, not in the entity file
data class FooItem(
    val id: Long = 0,
    val field1: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun FooItem.toEntity(): FooEntity = FooEntity(
    id = id,
    field1 = field1,
    timestamp = timestamp
)

fun FooEntity.toDomainModel(): FooItem = FooItem(
    id = id,
    field1 = field1,
    timestamp = timestamp
)
```

### Repository Interface Template

```kotlin
interface FooRepository {
    fun getAll(): Flow<List<FooItem>>
    suspend fun insert(item: FooItem)
    suspend fun delete(item: FooItem)
}
```

### Use Case Template

```kotlin
class DoFooUseCase @Inject constructor(
    private val repository: FooRepository
) {
    suspend operator fun invoke(param: String): Result<FooItem> = runCatching {
        // business logic here
        val item = FooItem(field1 = param)
        repository.insert(item)
        item
    }
}
```

**Rules:**
- Use cases use `operator fun invoke()` so they are called as functions: `doFooUseCase(param)`.
- Use cases return `Result<T>` for operations that can fail.
- Use cases are injected into ViewModels, never into other Use Cases.
- No Android imports in domain layer.

### Enum Template

```kotlin
enum class FooType(val displayName: String) {
    ALPHA("Alpha"),
    BETA("Beta"),
    UNKNOWN("Unknown");

    companion object {
        fun fromValue(value: String): FooType =
            entries.find { it.name == value } ?: UNKNOWN
    }
}
```

---

## 8. Presentation Layer (UI)

### ViewModel Template

```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(
    private val doFooUseCase: DoFooUseCase,
    private val fooRepository: FooRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            fooRepository.getAll()
                .map { items -> /* transform */ items }
                .collect { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
        }
    }

    fun onSomeAction(param: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            doFooUseCase(param)
                .onSuccess { item ->
                    _uiState.update { it.copy(isLoading = false, result = item) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}

data class FooUiState(
    val items: List<FooItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: FooItem? = null
)
```

**ViewModel rules:**
- Always `_uiState` (private mutable) + `uiState` (public StateFlow).
- Use `_uiState.update { it.copy(...) }` — never reassign the whole state.
- All async work in `viewModelScope.launch`.
- IO in Room/DataStore is handled by the suspend functions themselves; add `Dispatchers.Default` only for CPU-bound work (e.g., ZXing encoding, image processing).
- Override `onCleared()` only when cleaning up resources (ToneGenerator, camera, etc.).
- No Context references inside ViewModels — use `@ApplicationContext` only in DI-provided classes.

### Screen (Composable) Template

```kotlin
@Composable
fun FooScreen(
    viewModel: FooViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Foo") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                FooContent(
                    items = uiState.items,
                    onItemClick = onNavigateToDetail
                )
            }

            uiState.errorMessage?.let { message ->
                // show snackbar or error state
            }
        }
    }
}

@Composable
private fun FooContent(
    items: List<FooItem>,
    onItemClick: (Long) -> Unit
) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            FooListItem(item = item, onClick = { onItemClick(item.id) })
        }
    }
}
```

**Screen rules:**
- `hiltViewModel()` is the default — never pass ViewModels as constructor parameters.
- Collect state with `collectAsState()` at the top of the composable.
- Split large screens into private `@Composable` sub-functions.
- `Scaffold` wraps every screen; `TopAppBar` if needed.
- Navigation callbacks are lambdas passed into the composable, not handled inside it.
- No business logic inside Composables — all logic stays in the ViewModel.

### Reusable Component Template

```kotlin
@Composable
fun FooListItem(
    item: FooItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // content
        }
    }
}
```

**Component rules:**
- Always include a `modifier: Modifier = Modifier` parameter.
- Use Material 3 components (`Card`, `Button`, `Text`, `Icon`, `Chip`, etc.).
- Never hardcode colors — use `MaterialTheme.colorScheme.*`.
- Never hardcode text styles — use `MaterialTheme.typography.*`.

### Theme

```kotlin
// Color.kt — define all brand colors here
val PrimaryBlue = Color(0xFF...)

// Theme.kt — apply via MaterialTheme
@Composable
fun ScanProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## 9. Dependency Injection (Hilt)

### Application & Entry Points

```kotlin
@HiltAndroidApp
class ScanProApp : Application()          // app-level init

@AndroidEntryPoint
class MainActivity : ComponentActivity()  // only Activity
```

### Module Templates

**Object module** (for `@Provides`):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "scanpro.db").build()

    @Provides
    @Singleton
    fun provideFooDao(db: AppDatabase): FooDao = db.fooDao()
}
```

**Abstract module** (for `@Binds`):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFooRepository(impl: FooRepositoryImpl): FooRepository
}
```

**Rules:**
- All application-scoped singletons use `@Singleton` + `SingletonComponent`.
- Use `@Binds` for interface → implementation bindings (abstract module).
- Use `@Provides` for third-party classes (Room, DataStore, Retrofit).
- Never use `@HiltViewModel` without corresponding module for its dependencies.
- Inject `@ApplicationContext Context` — never `Activity` context into singletons.

---

## 10. Navigation

### Route Definitions (`NavigationKeys.kt`)

```kotlin
sealed class Screen(val route: String) {
    object Scanner  : Screen("scanner")
    object Generator: Screen("generator")
    object History  : Screen("history")
    object Settings : Screen("settings")
    object Detail   : Screen("detail/{itemId}") {
        fun createRoute(itemId: Long) = "detail/$itemId"
    }
}
```

### NavHost Setup (`Navigation.kt`)

```kotlin
@Composable
fun MainNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Scanner.route) {

        composable(Screen.Scanner.route) {
            ScannerScreen(onNavigateToDetail = { id ->
                navController.navigate(Screen.Detail.createRoute(id))
            })
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            DetailScreen(itemId = itemId, onBack = { navController.popBackStack() })
        }
    }
}
```

**Navigation rules:**
- Add new screens to the `Screen` sealed class first.
- Dynamic routes use `{paramName}` in the route string + `navArgument` definition.
- Bottom nav items use `launchSingleTop = true` + `restoreState = true`.
- Screens receive navigation callbacks as lambdas — they don't hold a NavController.

---

## 11. OOP Principles & Patterns

### Class Hierarchy

```
ViewModel (androidx)
  └── ScannerViewModel, GeneratorViewModel, HistoryViewModel,
      SettingsViewModel, DetailViewModel

RoomDatabase (androidx)
  └── AppDatabase

interface BarcodeRepository
  └── BarcodeRepositoryImpl

interface BarcodeDao  (generated by Room)

interface ImageAnalysis.Analyzer (CameraX)
  └── BarcodeAnalyzer
```

### Sealed Classes

Used for **navigation routes** and **UI state variants** when a feature has distinct states:

```kotlin
sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Result(val item: BarcodeItem) : ScanState()
    data class Error(val message: String) : ScanState()
}
```

### Enums with Behavior

Enums carry properties and companion factory methods:

```kotlin
enum class BarcodeFormatType(val displayName: String, val is2D: Boolean) {
    QR_CODE("QR Code", true),
    CODE_128("Code 128", false),
    UNKNOWN("Unknown", false);

    companion object {
        fun fromMlKitFormat(format: Int): BarcodeFormatType = ...
        fun toZxingFormat(type: BarcodeFormatType): BarcodeFormat = ...
    }
}
```

### Extension Functions for Mapping

Mappers live in the domain model file, not in entity or repository:

```kotlin
// in domain/model/BarcodeItem.kt
fun BarcodeItem.toEntity(): BarcodeEntity = ...
fun BarcodeEntity.toDomainModel(): BarcodeItem = ...
```

### Use Case as Callable

Use cases use `operator fun invoke()` to be called as functions:

```kotlin
val result = scanBarcodeUseCase(rawValue)   // not scanBarcodeUseCase.execute(rawValue)
```

---

## 12. State Management & Reactive Patterns

### StateFlow Pattern

```kotlin
// ViewModel
private val _uiState = MutableStateFlow(FooUiState())
val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

// Update
_uiState.update { current -> current.copy(isLoading = true) }

// Collect in Composable
val uiState by viewModel.uiState.collectAsState()
```

### Combining Flows

```kotlin
// When UI state depends on multiple reactive sources
combine(
    repository.getAllItems(),
    repository.settingsFlow
) { items, settings ->
    FooUiState(items = items, showFavorites = settings.showFavorites)
}.collect { state ->
    _uiState.value = state
}
```

### Coroutine Dispatchers

| Work type | Dispatcher |
|---|---|
| Room reads/writes | implicit (Room handles) |
| DataStore reads/writes | implicit (DataStore handles) |
| ZXing barcode encoding | `Dispatchers.Default` |
| File I/O (export/import) | `Dispatchers.IO` |
| UI updates | `Dispatchers.Main` (implicit in collectAsState) |

```kotlin
viewModelScope.launch(Dispatchers.Default) {
    val bitmap = generateBarcodeUseCase(content, format)
    withContext(Dispatchers.Main) {
        _uiState.update { it.copy(barcodeBitmap = bitmap) }
    }
}
```

### Error Handling

```kotlin
viewModelScope.launch {
    runCatching { someUseCase(param) }
        .onSuccess { result -> _uiState.update { it.copy(data = result) } }
        .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
}
```

---

## 13. Code Style Rules

### General

- **No comments** unless the WHY is non-obvious (workaround, hidden constraint, subtle invariant).
- No TODO/FIXME left in committed code.
- No unused imports, variables, or functions.
- All files end with a newline.

### Kotlin Specifics

- Prefer `data class` for models and UI state.
- Use `object` for singletons and companion factories.
- Use `val` everywhere possible; `var` only when mutation is genuinely needed.
- Prefer expression bodies for single-expression functions:
  ```kotlin
  fun getDisplayName(): String = type.displayName   // ✓
  fun getDisplayName(): String { return type.displayName }  // ✗
  ```
- Use `when` instead of `if-else` chains for multiple conditions.
- Use `?.let { }` / `?: return` for null handling — no `!!` except in test code.
- String templates over concatenation: `"Hello $name"` not `"Hello " + name`.

### Compose Specifics

- Every top-level composable has a `@Preview` annotation in debug/preview scope.
- Modifier chain order: size → layout → background → padding → click.
- Extract repeated UI chunks into private `@Composable` functions in the same file.
- Use `remember` and `derivedStateOf` to avoid redundant recomposition.
- State hoisting: state lives in ViewModel, not inside composables.

### Resource Naming (`res/`)

| Resource | Pattern | Example |
|---|---|---|
| String | `snake_case` | `scanner_title` |
| Color | `color_name` | `primary_blue` |
| Drawable | `ic_{name}` / `bg_{name}` | `ic_scan`, `bg_card` |
| Dimension | `{component}_{property}` | `card_corner_radius` |

---

## 14. Feature Addition Checklist

When adding a new feature (e.g., "Analytics"):

**Data Layer** (only if new persistence needed)
- [ ] Create `AnalyticsEntity.kt` in `data/local/db/`
- [ ] Create `AnalyticsDao.kt` in `data/local/db/`
- [ ] Add entity to `AppDatabase` entities list + bump version
- [ ] Create `AnalyticsRepositoryImpl.kt` in `data/repository/`
- [ ] Add DAO provider to `DatabaseModule.kt`
- [ ] Add `@Binds` to `RepositoryModule.kt`

**Domain Layer**
- [ ] Create `Analytics.kt` domain model in `domain/model/` (with mappers if entity exists)
- [ ] Create `AnalyticsRepository.kt` interface in `domain/repository/`
- [ ] Create `{Action}AnalyticsUseCase.kt` in `domain/usecase/`

**Presentation Layer**
- [ ] Create `ui/analytics/AnalyticsScreen.kt`
- [ ] Create `ui/analytics/AnalyticsViewModel.kt` with `AnalyticsUiState` data class
- [ ] Add `Screen.Analytics` to `NavigationKeys.kt`
- [ ] Add `composable(Screen.Analytics.route) { ... }` to `Navigation.kt`
- [ ] Add bottom nav entry if it's a top-level feature

**Quality**
- [ ] New domain model has no Android imports
- [ ] ViewModel only uses `viewModelScope`, no manual thread management
- [ ] Repository is injected as interface, not implementation
- [ ] All new `@Composable` functions have a `modifier: Modifier = Modifier` parameter
