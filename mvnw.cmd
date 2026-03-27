@echo off
setlocal

if not defined JAVA_HOME (
	if exist "E:\cong_cu\jdk-21\bin\java.exe" (
		set "JAVA_HOME=E:\cong_cu\jdk-21"
	)
)

if defined JAVA_HOME (
	set "PATH=%JAVA_HOME%\bin;%PATH%"
)

mvn %*
endlocal
