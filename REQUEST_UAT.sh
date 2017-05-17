#!/bin/ksh


###################################################### 
THIS_HOST=$(hostname -s)
THIS_SCRIPT="$0"
THIS_SCRIPT_SHORT=$( basename $0 )
DT="date +%Y%m%d%H%M%s"

######################################################
# FUNCTION: usage, no return
function usage
{
	echo "usage :"
	echo 
	echo "${THIS_SCRIPT_SHORT} -v <version_id>"
	echo 
	exit 3
}
######################################################
# FUNCTION: err
# $1 - prefix
# $2 - message
# $3 - severity, to add empty lines before and after
function err_msg
{
	[ -n "$3" ] echo 
		echo "$($DT): ${1} : ${THIS_SCRIPT_SHORT%.*}: ${2}"
	[ -n "$3" ] && echo
}

######################################################
# FUNCTION: CALL_JENKINS
function call_jenkins
{
#	curl -X POST --user admin:1q2w3e4r ${JENKINS_URL}?token=TOKEN&&WORK_SPACE_DIR=$WORK_SPACE_BUILD&&PROJECT_NAME=$PROJ&&BAR_FILE_NAME=${PROJ}.bar
	curl -X POST --user admin:1q2w3e4r ${JENKINS_URL} -d "Version_Id=${Version_Id}"
#	curl -X GET --user admin:1q2w3e4r http://9.112.244.119:8080/job/Upload_Artifacts_UCD/job/Job_Start/lastBuild/consoleText 
}
######################################################
# MAIN
#####################################################

JENKINS_URL="http://9.112.244.119:8080/job/Deployment_Automation_Pipeline/job/Test_Pipeline/buildWithParameters"

while [[ $# -gt 1 ]]
do
	case $1 in 
		"-v") 
			shift
			Version_Id=$1
			shift
			;;
		*)
			usage
			;;
	esac
done

if [[ -z "${Version_Id}" ]]; then 
	usage
else 
	call_jenkins
fi
		
