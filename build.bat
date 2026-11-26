@echo off
rem Builds the plugin. Requires JDK 21 and Gradle (or a generated wrapper).
if exist gradlew.bat (
    call gradlew.bat build
) else (
    gradle build
)
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)
echo Done. JAR is in build\libs\
pause
