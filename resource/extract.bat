echo %1 %2 %3 %4
SET tool_Dir=C:\Program Files\IBM\TeamConcertBuild\scmtools\eclipse
SET rtc_workspaceName=%2
SET rtc_componentName=%3
SET target_Dir=%4

for /f %%A in (%1) do (
	if not exist "%target_Dir%"%%~pA (mkdir "%target_Dir%"%%~pA)
	for /F "tokens=2 delims=[]" %%X in ('call "%tool_Dir%\scm" list remotefiles -r dsw -i "%rtc_workspaceName%" "%rtc_componentName%" %%A') do (
		call "%tool_Dir%\scm" get file -r dsw -w "%rtc_workspaceName%" -c "%rtc_componentName%" -f %%A %%X "%target_Dir%"%%~pA%%~nxA
	)
)
copy C:\Users\Administrator\.jenkins\workspace\Deployment_Automation_Pipeline\resource\sfilelist.txt %target_Dir%
"%tool_Dir%\scm" logout -r dsw
exit 0
