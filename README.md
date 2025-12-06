# AnyLLM

AnyLLM is a native Android application that enables connection to arbitrary Large Language Model (LLM) API endpoints through a configurable, template-based approach. The application decouples the chat interface from backend service providers, allowing users to define custom API configurations without modifying application code.

## Features

- **Universal API Compatibility**: Connect to any LLM service (OpenAI, Anthropic, Moonshot, DeepSeek, self-hosted endpoints, etc.) through configurable profiles
- **Template-Based Request Building**: Define request body templates with `{{variable}}` placeholder syntax for dynamic parameter injection
- **JSONPath Response Extraction**: Extract response content from heterogeneous API response structures using JSONPath expressions
- **Server-Sent Events (SSE) Support**: Real-time streaming output with incremental text rendering
- **Multi-Session Management**: Create, switch, and manage multiple conversation sessions per profile
- **Secure Credential Storage**: API keys encrypted using Android Keystore and EncryptedSharedPreferences
- **Localization**: English and Chinese language support

## System Requirements

- Android 9.0 (API level 28) or higher
- Network connectivity for API communication

## Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────────┐
│            Presentation Layer           │
│     (Jetpack Compose + ViewModel)       │
├─────────────────────────────────────────┤
│              Domain Layer               │
│         (Repository Pattern)            │
├─────────────────────────────────────────┤
│               Data Layer                │
│    (Room Database + DataStore + API)    │
└─────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology |
|-----------|------------|
| UI Framework | Jetpack Compose with Material 3 |
| Architecture | MVVM with Hilt dependency injection |
| Local Storage | Room Database |
| Preferences | DataStore + EncryptedSharedPreferences |
| Networking | OkHttp with SSE support |
| Serialization | Kotlinx Serialization |
| Response Parsing | Jayway JsonPath |

## Configuration

### Profile Structure

Each LLM profile consists of the following parameters:

| Field | Description |
|-------|-------------|
| `name` | Display name for the profile |
| `baseUrl` | API endpoint URL |
| `method` | HTTP method (GET, POST, PUT, PATCH, DELETE) |
| `headers` | Key-value pairs for request headers |
| `bodyTemplate` | JSON template with placeholder variables |
| `responseJsonPath` | JSONPath expression to extract response content |
| `streamEnabled` | Enable SSE streaming mode |
| `streamJsonPath` | JSONPath for streaming response chunks |
| `timeoutSeconds` | Request timeout in seconds |

### Template Variables

The following variables are automatically injected during request construction:

| Variable | Description |
|----------|-------------|
| `{{api_key}}` | API key stored for the profile |
| `{{messages}}` | JSON array of conversation messages |
| `{{stream}}` | Boolean indicating streaming mode |

### Example Configuration

OpenAI-compatible API configuration:

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{api_key}}
```

**Body Template:**
```json
{
  "model": "gpt-4",
  "messages": {{messages}},
  "stream": {{stream}}
}
```

**Response JSONPath:**
```
$.choices[0].message.content
```

**Stream JSONPath:**
```
$.choices[0].delta.content
```

## Project Structure

```
com.anzu.anyllm/
├── data/
│   ├── database/       # Room database and DAOs
│   ├── entity/         # Database entities
│   ├── preferences/    # DataStore and encrypted preferences
│   └── repository/     # Data repositories
├── di/                 # Hilt dependency injection modules
├── model/              # Domain models
├── network/            # API client and network utilities
├── ui/
│   ├── navigation/     # Navigation graph
│   ├── screen/         # Composable screens
│   └── theme/          # Material theme definitions
└── viewmodel/          # ViewModels
```

## Building

### Prerequisites

- Android Studio Ladybug or later
- JDK 17
- Android SDK 36

### Build Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build and run on device or emulator

```bash
./gradlew assembleDebug
```

## Data Storage

- **Conversation Data**: Stored locally in SQLite database via Room
- **API Keys**: Encrypted using AES-256-GCM via Android Keystore
- **User Preferences**: Stored in DataStore

No data is transmitted to external servers beyond the configured LLM API endpoints.

## License

This project is provided for educational and personal use.

