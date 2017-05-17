#!groovy

	def call_ucd_deploy(application, environment, process, component, version) {

		def import_version_status = httpRequest authentication: 'auth_dswimdevops',  contentType: 'APPLICATION_JSON',  httpMode: 'PUT', requestBody: '''{ "component": '''+ application +''', "properties": { "versionOrTag":"", "versionName": '''+ version +'''} }''', url: 'https://podbdevops.mkm.dst.ibm.com:8443/cli/component/integrate', validResponseCodes: '200:400'
			
		// wait for the version import completed 
		for (int times = 0; times < 50; times++) {
			sleep 5
			def judge = httpRequest acceptType: 'TEXT_PLAIN',authentication: 'auth_dswimdevops', url: 'https://podbdevops.mkm.dst.ibm.com:8443/cli/version/getVersionId?component=DSW_IM_DEVOPS_TEST&version=vvvv', validResponseCodes: '200:400'
			if(judge.status == 200) break
		}

    // deploy
		def deploy_status = httpRequest  contentType: 'APPLICATION_JSON', httpMode: 'PUT',  requestBody: '''{  "application": '''+ application +''',  "applicationProcess": '''+ process +''',  "environment": '''+ environment +''',  "versions": [{  "version": '''+ version +''',  "component": '''+ component +'''}]}''', url: 'https://podbdevops.mkm.dst.ibm.com:8443/cli/applicationProcessRequest/request'
	}

int version_num=1

/*
def version_num, version_string
node('master') {
	// fetch the filelist
	try {
		wrap([$class: 'MaskPasswordsBuildWrapper']) {
			def is_folder_exists = fileExists '../Upload_Artifacts_To_UCD'
			if (is_folder_exists) {
				dir ('../Upload_Artifacts_To_UCD') {
					deleteDir()
				}
			}
			if (isUnix()) {
				sh "java -jar ../Pipeline_Start@script/tools/FetchFiles.jar $RepositoryURI $user $Password \"$ProjectAreaName\" ../Upload_Artifacts_To_UCD $WorkItemID"
				//sh "ls -F ../Upload_Artifacts_To_UCD/ | grep /$ > ../Versions"
	      //sh "cat ../Versions | sed \"s/\///g\" > ../Versions"
				sh 'ls ../Upload_Artifacts_To_UCD > ../Versions'
				// for debug 
				sh 'ls ../Upload_Artifacts_To_UCD/'
			} else {
				bat "dir"
				bat "java -jar ..\\Test_Jenkinsfile@script\\tools\\FetchFiles.jar $RepositoryURI $user $Password \"$ProjectAreaName\" ..\\Upload_Artifacts_To_UCD\\ $WorkItemID"
				bat 'dir ..\\Upload_Artifacts_To_UCD\\ /b > ..\\Versions'
			}
		}
	} catch (e) {
		echo 'err in fetch the filelist'
		throw e
	}

	// get the versions && workItem
	try {
		version_string = readFile("../Versions").split("\n")
		version_num = version_string.length
		println "-----------" + version_num
	} catch (e) {
		echo 'err in get the versions from file'
		throw e
	}
}
*/

// loop for every workItem
for (int i = 0; i < version_num; i++) {
/*
	def version_folder = version_string[i].trim()
	def workItem_related_id = version_folder.split("_")[0]
	def ucd_param_map = [:]

	// read the filelist and configure file(sfilelist.txt && param.txt) by workItemId.
	// define the ucd param map: UCD_Environment, UCD_Component, UCD_Process, UCD_Resource, Resource_conname, Workitem_Id....
	node('master') {
		def workItem_files_list = readFile("../Upload_Artifacts_To_UCD/${version_folder}/${version_folder}/sfilelist.txt").split("\n")
		def workItem_files_num = workItem_files_list.length

		def	ucd_param_string = readFile("../Upload_Artifacts_To_UCD/${version_folder}/${version_folder}/param.txt").split("\n")
		def ucd_param_num = ucd_param_string.length
		for (int j = 0; j < ucd_param_num; j++) {
			def ucd_param_name = ucd_param_string[j].split(":")[0].trim()
			def ucd_param_value = ucd_param_string[j].split(":")[1].trim()
			ucd_param_map."${ucd_param_name}" = ucd_param_value
		}
	}
*/

	// unit testing 
	/*
	stage('UT') {
		node('testing server') {
			try {
				wrap([$class: 'MaskPasswordsBuildWrapper']) {
					for (int file_index = 0; file_index < workItem_files_num; file_index++) {
						def file_dir = workItem_files_list[file_index].split("/")
						def file_name = file_dir[file_dir.length - 1]
						def file_name_sub = file_name.substring(0, file_name.lastIndexOf(".")).trim()
						sh "/root/DSW_IM/UTRunner.ksh -o ${file_name_sub} -s ${workItem_related_id} -l test_log"
						println("--------------- Unit Testing LOGS ---------------")
						sh 'cat /root/DSW_IM/logs/test_log'
						println("-------------------------------------------------")
					}
				}
			} catch (e) {
				echo 'This will run only if failed!'
			} finally {
				def currentResult = currentBuild.result ?: 'success'
				if (currentResult == 'UNSTABLE') {
					echo 'This will run only if the run was marked as unstable'
				}
				
				echo 'This will always run'
			}
		}
	}
	*/
		
	// check out the code from the scm	
	stage('Check Out Artifacts from SCM'){
		node('master'){
/*
			wrap([$class: 'MaskPasswordsBuildWrapper']) {
				println("----------Logs about checking out artifacts from scm ---------------")				
				if (isUnix()) {
				} else {
					//bat "..\\resource\\loadfiles.bat ..\\Upload_Artifacts_To_UCD\\${version_folder}\\${version_folder}\\sfilelist.txt ${ucd_param_map."Stream"} ${ucd_param_map."Component"} ..\\Upload_Artifacts_To_UCD\\${version_folder}\\${version_folder} $RepositoryURI $user $password"
				}
				println("-----------------------------------------------------")
			}
			// update the repository 
			def repository_folder_exist = fileExists '../repository'
			if (!repository_folder_exist) {
				if (isUnix()) sh 'mkdir ../repository'
				else bat 'mkdir ..\\repository'
			}

			def version_repository_exist = fileExists "../repository/${workItem_related_id}"
			def sub_version_repository_exist = fileExists "../repository/${workItem_related_id}/${workItem_related_id}"
			def version_repository_sfilelist_exist = fileExists "../repository/${workItem_related_id}/${workItem_related_id}/sfilelist.txt"
			if (isUnix()) {
				if (!version_repository_exist) sh "mkdir ../repository/${workItem_related_id}"
				if (!sub_version_repository_exist) sh "mkdir ../repository/${workItem_related_id}/${workItem_related_id}"
			  if (!version_repository_sfilelist_exist) sh "cp ../Upload_Artifacts_To_UCD/${version_folder}/${version_folder}/sfilelist.txt  ../repository/${workItem_related_id}/${workItem_related_id}"
			 	sh "cp -r ../Upload_Artifacts_To_UCD/${version_folder}/${version_folder} ../repository/${workItem_related_id}/${workItem_related_id}"
			 	sh "cat -r ../Upload_Artifacts_To_UCD/${version_folder}/${version_folder}/sfilelist.txt  ../repository/${workItem_related_id}/${workItem_related_id}/sfilelist.txt | sort | uniq > ../repository/${workItem_related_id}/${workItem_related_id}/sfilelist.txt"
			}
			else {
				if (!version_repository_exist) bat "mkdir ..\\repository\\${workItem_related_id}"
				if (!sub_version_repository_exist) bat "mkdir ..\\repository\\${workItem_related_id}\\${workItem_related_id}"
				def latestVersion = "C:/Users/Administrator/.jenkins/workspace/Deployment_Automation_Pipeline/Upload_Artifacts_To_UCD/${version_folder}/${version_folder}/sfilelist.txt"
				def dest = "C:/Users/Administrator/.jenkins/workspace/Deployment_Automation_Pipeline/repository/${workItem_related_id}/${workItem_related_id}/sfilelist_tmp.txt"
				def list1 = new File(dest)
				def list2 = new File(latestVersion)
		
				HashSet list = new HashSet()
	
				if (list1.exists()) {
					list1.eachLine { line->
						if (line)  {
							list.add(line)
						}
					}
				}
		
				def file = new File(dest)
				if (file.exists()) file.delete()

				def printwriter = file.newPrintWriter()
				list.each { k->
					printwriter.write(k + '\r\n')
				}
				printwriter.flush()
				printwriter.close()
			}
		}

		node ('master') {
			if (isUnix()) { 
				//TODO git operate
		  }
			else {
				bat "xcopy ..\\Upload_Artifacts_To_UCD\\${version_folder}\\${version_folder}  ..\\repository\\${workItem_related_id}\\${workItem_related_id} /s /e /y"
				bat "type ..\\repository\\${workItem_related_id}\\${workItem_related_id}\\sfilelist_tmp.txt > ..\\repository\\${workItem_related_id}\\${workItem_related_id}}\\sfilelist.txt"
				bat "del ..\\repository\\${workItem_related_id}\\${workItem_related_id}\\sfilelist_tmp.txt"
			}
*/
		}
	}


	stage('Update the WorkItem Status(Close)') {
		node('master') { //TODO need args!!!  
			try {
				//call_ucd_deploy()
			}	catch (e) {
				throw e
			}
		}
	}

	stage('Upload the Artifacts to UCD -') {
		node('master'){
			//TODO need args!!!
			try {
				//call_ucd_deploy()
			}	catch (e) {
				throw e
			}
		}
	}

	stage("Update the WorkItem Status") {
		node('master'){ 
			//TODO need args!!
			try {
				//call_ucd_deploy()
			}	catch (e) {
				throw e
			}
	 	}
	}
/*
	stage('Regression Testing'){
			node ('testing server') {
				try {
					println("--------Logs about regression Testing ------------")
					sh '/root/DSW_IM/exec.ksh'
					println("-------- tc_log_FVT_5 -----------")
					sh 'cat /root/DSW_IM/tc_log_FVT_5.log'
					println("-------- tc_log_FVT_6 -----------")
					sh 'cat /root/DSW_IM/tc_log_FVT_6.log'
					println("---------------------------------")
				} catch (e) {
					throw e
				}
			}
	}	
*/
}
