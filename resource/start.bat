echo %1 %2 %3
SET tool_Dir=C:\Program Files\IBM\TeamConcertBuild\scmtools\eclipse
SET rtc_repositoryURI=%1
SET rtc_userId=%2
SET rtc_password=%3

 "%tool_Dir%\scm" login -r %rtc_repositoryURI% -u %rtc_userId% -P %rtc_password% -n dsw

exit 0
