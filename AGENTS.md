# SkinsRestorer Agent Guidelines

## Build Commands

- **Full build**: `./gradlew build`
- **Build without tests**: `./gradlew build -x test`
- **Clean build**: `./gradlew clean build`
- **Compile only**: `./gradlew compileJava`

## Test Commands

- **Run all tests**: `./gradlew test`
- **Run single test class**: `./gradlew test --tests "ClassName"`
- **Run single test method**: `./gradlew test --tests "ClassName.methodName"`
- **Run tests with coverage**: `./gradlew test jacocoTestReport`

## Lint & Format Commands

- **Check formatting**: `./gradlew spotlessCheck`
- **Apply formatting**: `./gradlew spotlessApply`
- **Check static analysis**: `./gradlew compileJava` (ErrorProne enabled)

## Code Style Guidelines

### General

- **Language**: Java 25
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Encoding**: UTF-8
- **License header**: Required on all Java files (auto-applied by Spotless)

### Imports

- Organize imports alphabetically
- Separate java.* imports from others with blank line
- Use static imports sparingly, only for constants

### Naming Conventions

- **Classes**: PascalCase
- **Methods/Variables**: camelCase
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase with dots

### Lombok Usage

- Use Lombok annotations (@Data, @AllArgsConstructor, etc.)
- JetBrains null annotations enabled (`lombok.addNullAnnotations = jetbrains`)
- Prefer constructor injection over field injection

### Error Handling

- Use checked exceptions for recoverable errors
- Use runtime exceptions for programming errors
- Log errors appropriately with SRLogger
- Validate inputs with Objects.requireNonNull()

### Testing

- Use JUnit 5 with Mockito for unit tests
- Use TestContainers for integration tests
- Follow naming pattern: `*Test.java` for unit tests
- Use SRExtension for shared test setup

## Java conventions

- Import Java types instead of using fully qualified class names in code.
