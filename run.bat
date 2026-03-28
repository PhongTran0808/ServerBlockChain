@echo off
echo Building project...
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo BUILD FAILED
    pause
    exit /b 1
)
echo.
echo Starting Spring Boot on port 7071...
java -jar target\cuutro-backend-0.0.1-SNAPSHOT.jar
