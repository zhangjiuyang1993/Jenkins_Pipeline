echo %1 %2 %3 %4 %5 %6 %7
SET tool_Dir=C:\Program Files\IBM\TeamConcertBuild\scmtools\eclipse
SET filelist_Path=%1
SET rtc_workspaceName=%2
SET rtc_componentName=%3
SET target_Dir=%4
SET rtc_repositoryURI=%5
SET rtc_userId=%6
SET rtc_password=%7

"%tool_Dir%\scm" login -r %rtc_repositoryURI% -u %rtc_userId% -P %rtc_password% -n dsw

for /f %%A in (%1) do (
	if not exist "%target_Dir%"%%~pA (mkdir "%target_Dir%"%%~pA)
	for /F "tokens=2 delims=[]" %%X in ('call "%tool_Dir%\scm" list remotefiles -r dsw -i "%rtc_workspaceName%" "%rtc_componentName%" %%A') do (
		call "%tool_Dir%\scm" get file -r dsw -w "%rtc_workspaceName%" -c "%rtc_componentName%" -f %%A %%X "%target_Dir%"%%~pA%%~nxA
	)
)
"%tool_Dir%\scm" logout -r dsw
exit 0
