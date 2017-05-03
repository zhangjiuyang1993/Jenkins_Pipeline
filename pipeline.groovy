timestamps {
	/****************************************************
	 *Function: UT 
	 *
	 ****************************************************/
	/*
	def flag
   	node('testing server') {
    	stage name: "UT"
        sh '/root/DSW_IM/jenkins_test.sh "${UT_Option}"'
        if ("sh 'echo \$?'") {
            echo "1"
        } else {
            echo "0"
        }
    }
	 */
    
    /*****************************************************
     *Function: Catch log from UCD
	 *
     *****************************************************/
	node('testing server') {

		//request the component by ucd rest
		build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log',
       		parameters:[ 					
      			[$class: 'StringParameterValue', name: 'request_url', value: "https://dswimdevops.cn.ibm.com:8443/cli/component/info?component=Update_WorkItem_Status"]
            ]
        sh "jq .id ../Catch_UCD_Log/Out.json | sed 's/\"//g' > tmp"
        component_ID = readFile("./tmp").split("\n")[0]
        
		//find the property componentProcessRequest by ucd rest(query by version)
        build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log',
            parameters:[ 					
                [$class: 'StringParameterValue', name: 'request_url', value: "https://dswimdevops.cn.ibm.com:8443/rest/deploy/componentProcessRequest/table?rowsPerPage=10&pageNumber=1&orderField=calendarEntry.scheduledDate&sortType=desc&filterFields=component.id&filterValue_component.id="+component_ID+"&filterType_component.id=eq&filterClass_component.id=UUID&outputType=BASIC&outputType=LINKED&outputType=EXTENDED"]
            ]
        sh 'cd ../Catch_UCD_Log;./jq_parse_component_process_json.ksh v213-20170419114056;cat ./tmp'
        component_Process_Request_ID = readFile("../Catch_UCD_Log/tmp")
        
		//request the componentprocessProcessRequest 
        build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log',
            parameters:[ 					
                [$class: 'StringParameterValue', name: 'request_url', value: "https://dswimdevops.cn.ibm.com:8443/rest/workflow/componentProcessRequest/"+component_Process_Request_ID]
            ]    
        sh "jq .workflowTraceId ../Catch_UCD_Log/Out.json | sed 's/\"//g' > tmp"
        workflowTrace_ID = readFile("./tmp").split("\n")[0]
        sh "jq '.children | length' ../Catch_UCD_Log/Out.json > tmp"
        workflowTrace_Children_length = readFile("./tmp").split("\n")[0].toInteger()
        
		//get the actual log
        for (int i=0; i<workflowTrace_Children_length;i++) {

            sh "jq .children["+i+"].id ../Catch_UCD_Log/Out.json | sed 's/\"//g' > tmp"
            sh "echo ./tmp"
            children_ID = readFile("./tmp").split("\n")[0]
            
            build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log',
            parameters:[ 					
                [$class: 'StringParameterValue', name: 'request_url', value: "https://dswimdevops.cn.ibm.com:8443/rest/logView/trace/"+workflowTrace_ID+"/"+children_ID+"/"+"stdOut.txt"]
            ]
            sh 'cat ../Catch_UCD_Log/Out.json'
        }
    }
    


    /*****************************************************
     * Function : Check out Artifacts from RTC
     * args:
     * 1.RespositoryURI  
     * 2.user
     * 3.Password
     * 4.WorkItemID
     * 5.ProjectAreaName
     ****************************************************/    
    
	node('master') {
   		stage name: "Check Out Artifacts from SCM" 
        try {
            echo "Check Out"
            build job: 'Deployment_Automation_Pipeline/Check_Out_Artifacts_From_SCM',
                parameters:[ 					
                    [$class: 'StringParameterValue', name: 'RepositoryURI', value: "${RepositoryURI}"],
                    [$class: 'StringParameterValue', name: 'user', value: "${user}"],
                    [$class: 'StringParameterValue', name: 'Password', value: "${Password}"],
                    [$class: 'StringParameterValue', name: 'WorkItemID', value: "${WorkItemID}"],
                    [$class: 'StringParameterValue', name: 'ProjectAreaName', value: "${ProjectAreaName}"]
                ]
        } catch(err) {
            currentBuild.result = 'FAILURE'
        }
        
    }
    
    /*****************************************************
     * Function : Get Category and env of WorkItem
     * Description: map[workItemID, category;env]
     * category or env should not be null
     ****************************************************/
    def attribute_string
    def attribute_length
    def attribute_category
    def attribute_env
    def attribute_map = [:]

    node('master') {
        attribute_string = readFile("../resource/attribute.txt").split("\n")
        attribute_length = attribute_string.length
        for (int i = 0; i < attribute_length; i++) {
            def workItemId_tmp, category_tmp, env_tmp
            workItemId_tmp = attribute_string[i].split(";")[0].split(":")[1].trim()
            category_tmp = attribute_string[i].split(";")[1].split(":")[1].trim()
            env_tmp = attribute_string[i].split(";")[2].split(":")[1].trim()
            attribute_map."${workItemId_tmp}" = category_tmp + ";" + env_tmp
        }
    }

    /*****************************************************
     * Function : Deploy
     * Description:
     * 1.update the state of workItem(new...)
     * 2.Deploy(apply *.sql....)
     * 3.update the state fo workItem(close....)
     ****************************************************/    
    def version_string
    def version_num

    node('master') {
        version_string = readFile("../Versions").split("\n")
        version_num = version_string.length
    }
    
    for (int i = 0; i < version_num; i++) {
        def s = version_string[i].split("_")
        
        def attribute_tmp = attribute_map."${s[0]}"
        attribute_category = attribute_tmp.split(";")[0]
        attribute_env = attribute_tmp.split(";")[0]
        node{
            stage name: "Update the WorkItem Status(New) - ${s[0]}"
            try {
                build job: 'Deployment_Automation_Pipeline/Job_Update_WorkItem_Status',
                parameters:[ 					
                    [$class: 'StringParameterValue', name: 'RTC_URL', value: "${RepositoryURI}"],
                    [$class: 'StringParameterValue', name: 'WorkItem_Action', value: "Close"],
                    [$class: 'StringParameterValue', name: 'WorkItem_ID', value: s[0]],
                    [$class: 'StringParameterValue', name: 'WorkItem_New_Status', value: "Closed+-+Not+Completed"]
                ]
            } catch(err) {
            currentBuild.result = "FAILURE" 
            }  
        }
        
        node('testing server') {
            build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log'
            sh 'cat ../Catch_UCD_Log/Out.json'
        }

        node('master') {
            stage name: "Upload the Artificats to UCD - ${s[0]}"
            try {
                build job: 'Deployment_Automation_Pipeline/Upload_Artifacts_To_UCD',
                parameters:[ 					
                    [$class: 'StringParameterValue', name: 'versionID', value: version_string[0]],
                    [$class: 'StringParameterValue', name: 'UCD_Application', value: "${UCD_Application}"],
                    [$class: 'StringParameterValue', name: 'UCD_Component', value: "${UCD_Component}"],
                    [$class: 'StringParameterValue', name: 'UCD_Environment', value: "${UCD_Environment}"],
                    [$class: 'StringParameterValue', name: 'UCD_Process', value: "${UCD_Process}"]
                ]
            } catch(err) {
                currentBuild.result = "FAILURE"
            }      
        }
        
        node('testing server') {
            build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log'
            sh 'cat ../Catch_UCD_Log/Out.json'
        }

        node {
            stage name: "Update the WorkItem Status(Close) - ${s[0]}"
            try {
                build job: 'Deployment_Automation_Pipeline/Job_Update_WorkItem_Status',
                parameters:[ 					
                    [$class: 'StringParameterValue', name: 'RTC_URL', value: "${RepositoryURI}"],
                    [$class: 'StringParameterValue', name: 'WorkItem_Action', value: "Close"],
                    [$class: 'StringParameterValue', name: 'WorkItem_ID', value: s[0]],
                    [$class: 'StringParameterValue', name: 'WorkItem_New_Status', value: "Closed+-+Not+Completed"]
                ]
            } catch(err) {
            currentBuild.result = "FAILURE" 
            }  
        }
        
        node('testing server') {
            build job: 'Deployment_Automation_Pipeline/Catch_UCD_Log'
            sh 'cat ../Catch_UCD_Log/Out.json'
        }
    }
}
