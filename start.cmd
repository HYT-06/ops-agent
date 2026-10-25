@echo off
rem ops-agent 构建与启动脚本
rem   start.cmd         构建并运行可执行 jar
rem   start.cmd dev     以 spring-boot:run 启动（配合 devtools 热更新）
rem   start.cmd build   只构建
rem   start.cmd test    运行全部测试
rem   start.cmd run     直接运行已构建的 jar
setlocal
cd /d "%~dp0"
set "JAR=ops-orchestrator\target\ops-orchestrator-0.1.0-SNAPSHOT.jar"
set "CMD=%~1"
if "%CMD%"=="" set "CMD=start"

if "%CMD%"=="build" (
    call mvn -DskipTests package
    exit /b %errorlevel%
)
if "%CMD%"=="test" (
    call mvn test
    exit /b %errorlevel%
)
if "%CMD%"=="dev" (
    call mvn -pl ops-orchestrator -am spring-boot:run
    exit /b %errorlevel%
)
if "%CMD%"=="run" (
    if not exist "%JAR%" (
        echo 未找到 %JAR%，请先执行 start.cmd build
        exit /b 1
    )
    java -jar "%JAR%"
    exit /b %errorlevel%
)
if "%CMD%"=="start" (
    call mvn -DskipTests package
    if errorlevel 1 exit /b 1
    java -jar "%JAR%"
    exit /b %errorlevel%
)
echo 用法: start.cmd {start^|dev^|build^|test^|run}
exit /b 1
