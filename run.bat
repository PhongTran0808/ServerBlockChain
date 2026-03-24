@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2
set PATH=%JAVA_HOME%\bin;%PATH%
echo Using Java:
java -version
echo.
echo Starting Spring Boot on port 7071...
mvn spring-boot:run -DskipTests
