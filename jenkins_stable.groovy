timestamps {
	
	// fetch the filelist 
	node('master') {
		def dir = System.getProperty("user.dir")
		bat 'if exist C:\\Users\\Administrator\\.jenkins\\workspace\\Deployment_Automation_Pipeline\\Upload_Artifacts_To_UCD  (rd /s /q  C:\\Users\\Administrator\\.jenkins\\workspace\\Deployment_Automation_Pipeline\\Upload_Artifacts_To_UCD)'
		println System.getProperty("user.dir")
		bat "java -jar ..\\FetchFiles.jar $RepositoryURI $user $Password \"$ProjectAreaName\" ..\\Upload_Artifacts_To_UCD\\ $WorkItemID" 
		bat 'dir ..\\Upload_Artifacts_To_UCD\\ /b > ..\\Versions'
	}

	// get the vesion && workItem
	def version_num
    node('master') {
    	version_string = readFile("../Versions").split("\n")
        version_num = version_string.length
    }

	// loop for every workItem
	for (int i = 0; i < version_num; i++){
		//get the file list and configure file
		def files_array	
		def files_array_length
	
		def workItem_parent_id = version_string[i].split("_")[0]
		def s = version_string[i].split("_")

	    node('master') {
         	try {
				files_array = readFile("./Upload_Artifacts_To_UCD/${version_string[i].trim()}/sfilelist.txt}").split("\n")
				files_array_length = files_array.length
	         } catch(err){
	             currentBuild.result = 'FAILURE'
	         }
	     }
	
		// UT 

		node('testing server') {
			stage name: "UT"
			for (int file_index = 0; file_index < files_array_length; file_index++) {
				try {
					def file_dir = files_array[file_index].split("/")
					def file_name = file_dir[file_dir.lenght - 1]
					def file_name_sub = file_name.substring(0, file_name.lastIndexOf(".")).trim()
					sh "/root/DSW_IM/UTRunner.ksh -o ${file_name_sub} -s ${workItem_parent_id}"
				} catch(err) {
					currentBuild.result = 'FAILURE'
				}
			}
		}		


		// define the ucd param map: UCD_Environment, UCD_Component, UCD_Process, UCD_Resource, Resource_conname, WorkItem_Id
		def ucd_param_map = [:]
		node('master') {
			def ucd_param_string = readFile("../Upload_Artifacts_To_UCD/${version_string[i].trim()}/param.txt").split("\n")
			def ucd_param_num = ucd_param_string.length
			for (int j = 0; j < ucd_param_num; j++) {
				def ucd_param_name = ucd_param_string[j].split(":")[0].trim()
				def ucd_param_value = ucd_param_string[j].split(":")[1].trim()
				ucd_param_map."${ucd_param_name}" = ucd_param_value
			}
		}
	
		// check out the code from the SCM
		node('master') {
			stage name: "Check Out Artifacts from SCM"
			try {
				bat "..\\resource\\loadfiles.bat ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()}\\sfilelist.txt  ${ucd_param_map."Stream"} ${ucd_param_map."Component"} ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()}  $RepositoryURI $user $Password"	} catch(err) { currentBuild.result = 'FAILURE' } }
/*
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
*/		
		// update the workItem's status exec - In - Process
		node {
			stage name: "Update the WorkItem Status(New) - ${s[0]}"
			try {
				build job: 'Deployment_Automation_Pipeline/Job_Update_WorkItem_Status',
					parameters:[
					[$class: 'StringParameterValue', name: 'RTC_URL', value: "${RepositoryURI}"],
					[$class: 'StringParameterValue', name: 'WorkItem_Action', value: "Close"],
					[$class: 'StringParameterValue', name: 'WorkItem_ID', value: ucd_param_map."WorkItem_Id"],
					[$class: 'StringParameterValue', name: 'WorkItem_New_Status', value: "Closed+-+Not+Completed"],
					[$class: 'StringParameterValue', name: 'UCD_Application', value: "${UCD_Application}"],
					[$class: 'StringParameterValue', name: 'UCD_Component', value: "Update_WorkItem_Status"],
					[$class: 'StringParameterValue', name: 'UCD_Environment', value: ucd_param_map."UCD_Environment"],
					[$class: 'StringParameterValue', name: 'UCD_Process', value: "Update_WorkItem_Status_Envproc"]
				]
			} catch(err) {
				currentBuild.result = "FAILURE"
			}
		}
	
		// upload the artifacts && call ucd to deploy
		node('master') {
			stage name: "Upload the Artificats to UCD - ${s[0]}"
			try {
				build job: 'Deployment_Automation_Pipeline/Upload_Artifacts_To_UCD',
				parameters:[
					[$class: 'StringParameterValue', name: 'versionID', value: "${version_string[i]}"],
					[$class: 'StringParameterValue', name: 'UCD_Application', value: "${UCD_Application}"],
					[$class: 'StringParameterValue', name: 'UCD_Component', value: ucd_param_map."UCD_Component"],
					[$class: 'StringParameterValue', name: 'UCD_Environment', value: ucd_param_map."UCD_Environment"],
					[$class: 'StringParameterValue', name: 'UCD_Process', value: ucd_param_map."UCD_Process"],
					[$class: 'StringParameterValue', name: 'UCD_Resource', value: ucd_param_map."UCD_Resource"],
					[$class: 'StringParameterValue', name: 'UCD_Resource_Properties_conname', value: ucd_param_map."Resource_conname"],
					[$class: 'StringParameterValue', name: "WorkItem_Parent_ID", value: "${workItem_parent_id}"]
				]
			} catch(err) {
				currentBuild.result = "FAILURE"
			}
		}
	
		// update the workItem's status Done -
		node {
			stage name: "Update the WorkItem Status(Close) - ${s[0]}"
			try {
				build job: 'Deployment_Automation_Pipeline/Job_Update_WorkItem_Status',
					parameters:[
						[$class: 'StringParameterValue', name: 'RTC_URL', value: "${RepositoryURI}"],
						[$class: 'StringParameterValue', name: 'WorkItem_Action', value: "Close"],
						[$class: 'StringParameterValue', name: 'WorkItem_ID', value: ucd_param_map."WorkItem_Id"],
						[$class: 'StringParameterValue', name: 'WorkItem_New_Status', value: "Closed+-+Not+Completed"],
						[$class: 'StringParameterValue', name: 'UCD_Application', value: "${UCD_Application}"],
						[$class: 'StringParameterValue', name: 'UCD_Component', value: "Update_WorkItem_Status"],
						[$class: 'StringParameterValue', name: 'UCD_Environment', value: ucd_param_map."UCD_Environment"],
						[$class: 'StringParameterValue', name: 'UCD_Process', value: "Update_WorkItem_Status_Envproc"]
					]
			} catch(err) {
				currentBuild.result = "FAILURE"
			}
		}
	
		// regression testing
		node('testing server') {
			stage name: "Regression Testing"
			//try {
			//	sh '/root/DSW_IM/exec.ksh'
			//} catch (err) {
			//	currentBuild.result = 'SUCCESS'
			//}
		}
	}	
}

