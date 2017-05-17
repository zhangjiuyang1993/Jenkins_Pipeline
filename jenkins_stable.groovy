timestamps {

    // fetch the filelist
    node('master') {
        wrap([$class: 'MaskPasswordsBuildWrapper']) {
            def folder_exist = fileExists '../Upload_Artifacts_To_UCD'
           if (folder_exist) {
            //bat 'rd /s /q ..\\Upload_Artifacts_To_UCD'
            //bat 'mkdir ..\\Upload_Artifacts_To_UCD'
                dir('../Upload_Artifacts_To_UCD') {
                    deleteDir()
                }
            }
            bat 'mkdir ..\\Upload_Artifacts_To_UCD'
           //println System.getProperty("user.dir")
            bat "java -jar ..\\FetchFiles.jar $RepositoryURI $user $Password \"$ProjectAreaName\" ..\\Upload_Artifacts_To_UCD\\ $WorkItemID"
            bat 'dir ..\\Upload_Artifacts_To_UCD\\ /b > ..\\Versions'
        }
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
            files_array = readFile("../Upload_Artifacts_To_UCD/${version_string[i].trim()}/${version_string[i].trim()}/sfilelist.txt").split("\n")
            files_array_length = files_array.length
         }

        // UT
        
        node('testing server') {
            stage name: "Unit Testing"
            wrap([$class: 'MaskPasswordsBuildWrapper']) {
                for (int file_index = 0; file_index < files_array_length; file_index++) {
                    def file_dir = files_array[file_index].split("/")
                    def file_name = file_dir[file_dir.length - 1]
                    def file_name_sub = file_name.substring(0, file_name.lastIndexOf(".")).trim()
                    sh "/root/DSW_IM/UTRunner.ksh -o ${file_name_sub} -s ${workItem_parent_id} -l test_log"
                    println("------------------ Unit Testing LOGS ------------------------")
                    sh 'cat /root/DSW_IM/logs/test_log'
                    println("-------------------------------------------------------------")
                }
            }
        }
        
        // define the ucd param map: UCD_Environment, UCD_Component, UCD_Process, UCD_Resource, Resource_conname, WorkItem_Id
        def ucd_param_map = [:]
        node('master') {
            def ucd_param_string = readFile("../Upload_Artifacts_To_UCD/${version_string[i].trim()}/${version_string[i].trim()}/param.txt").split("\n")
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
            wrap([$class: 'MaskPasswordsBuildWrapper']) {
                println("--------------------LOGS About check out artifacts from SCM ----------------------")
                bat "..\\resource\\loadfiles.bat ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()}\\${version_string[i].trim()}\\sfilelist.txt  ${ucd_param_map."Stream"} ${ucd_param_map."Component"} ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()}\\${version_string[i].trim()}  $RepositoryURI $user $Password"
               println("----------------------------------------------------------------------------------")
            }
        }

        // update the repository
        node('master') {
            def repository_folder_exist = fileExists '../repository'
            if (!repository_folder_exist) {
                if (isUnix()) sh 'mkdir ../repository'
                else bat 'mkdir ..\\repository'
            }
            if (isUnix()) {}
            else {
                def version_repository_exist = fileExists "../repository/${s[0]}"
                if (!version_repository_exist) bat "mkdir ..\\repository\\${s[0]}"
                def sub_version_repository_exist = fileExists "../repository/${s[0]}/${s[0]}"
                if (!sub_version_repository_exist) bat "mkdir ..\\repository\\${s[0]}\\${s[0]}"


                def latestVersion = "C:/Users/Administrator/.jenkins/workspace/Deployment_Automation_Pipeline/Upload_Artifacts_To_UCD/${version_string[i].trim()}/${version_string[i].trim()}/sfilelist.txt"
                def dest = "C:/Users/Administrator/.jenkins/workspace/Deployment_Automation_Pipeline/repository/${s[0]}/${s[0]}/sfilelist_tmp.txt"
                def list1 = new File(dest)
                def list2 = new File(latestVersion)

                HashSet list = new HashSet()

                if(list1.exists()){
                    list1.eachLine {line->
                        if(line){
                               list.add(line)
                        }
                    }
                }
                if(list2.exists()) {
                    list2.eachLine { line ->
                        if (line) {
                            list.add(line)
                        }
                    }
                }
                def file = new File(dest)

                if(file.exists())
                    file.delete()

                def printwriter = file.newPrintWriter()

                list.each{k->
                    printwriter.write(k + '\r\n')
                }

                printwriter.flush()
                printwriter.close()

                    //bat "type ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()}\\sfilelist.txt >> ..\\repository\\${s[0]}\\sfilelist.txt"
                    //bat "for /f \"delimes=\" %%i in (..\\repository\\${s[0]}\\sfilelist.txt) do ( if not defined %%i set %%i=s & echo %%i>>..\\repository\\${s[0]}\\temp.txt)"
                //bat "xcopy ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()} ..\\repository\\${s[0]} /s /e /y"
                //bat "type ..\\repository\\${s[0]}\\sfilelist_tmp.txt > ..\\repository\\${s[0]}\\sfilelist.txt"
                //bat "del ..\\repository\\${s[0]}\\sfilelist_tmp.txt"
            }
        }

        node('master') {
            bat "xcopy ..\\Upload_Artifacts_To_UCD\\${version_string[i].trim()}\\${version_string[i].trim()} ..\\repository\\${s[0]}\\${s[0]} /s /e /y"
            bat "type ..\\repository\\${s[0]}\\${s[0]}\\sfilelist_tmp.txt > ..\\repository\\${s[0]}\\${s[0]}\\sfilelist.txt"
            bat "del ..\\repository\\${s[0]}\\${s[0]}\\sfilelist_tmp.txt"
        }

        // update the workItem's status exec - In - Process
        node {
            stage name: "Update the WorkItem Status(In-Process) - ${s[0]}"
            try {
                build job: 'Deployment_Automation_Pipeline/Job_Update_WorkItem_Status',
                    parameters:[
                    [$class: 'StringParameterValue', name: 'RTC_URL', value: "${RepositoryURI}"],
                    [$class: 'StringParameterValue', name: 'WorkItem_Action', value: "Execute"],
                    [$class: 'StringParameterValue', name: 'WorkItem_ID', value: ucd_param_map."WorkItem_Id"],
                    [$class: 'StringParameterValue', name: 'WorkItem_New_Status', value: "In-Process"],
                    [$class: 'StringParameterValue', name: 'UCD_Application', value: "DSW_IM_DevOps_Applicaitons"],
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

            //def database_name = ucd_param_map."Resource_conname"

            //httpRequest authentication: 'auth_dswimdevops', httpMode: 'PUT', url: " https://dswimdevops.cn.ibm.com:8443/cli/resource/setProperty?resource=/DB2Agent/db2agent_alan/DB2_Auto_Deployment&&application=DSW_IM_DevOps_Applications&&name=conname&&value=${database_name}"

            try {
                build job: 'Deployment_Automation_Pipeline/Upload_Artifacts_To_UCD',
                parameters:[
                    [$class: 'StringParameterValue', name: 'versionID', value: "${version_string[i].trim()}"],
                    [$class: 'StringParameterValue', name: 'UCD_Application', value: "DSW_IM_DevOps_Applicaitons"],
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
            stage name: "Update the WorkItem Status(Completed) - ${s[0]}"
            try {
                build job: 'Deployment_Automation_Pipeline/Job_Update_WorkItem_Status',
                    parameters:[
                        [$class: 'StringParameterValue', name: 'RTC_URL', value: "${RepositoryURI}"],
                        [$class: 'StringParameterValue', name: 'WorkItem_Action', value: "Done"],
                        [$class: 'StringParameterValue', name: 'WorkItem_ID', value: ucd_param_map."WorkItem_Id"],
                        [$class: 'StringParameterValue', name: 'WorkItem_New_Status', value: "Completed+-+Awaiting+Sign-off"],
                        [$class: 'StringParameterValue', name: 'UCD_Application', value: "DSW_IM_DevOps_Applicaitons"],
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
            println("--------------------LOGS about regression Testing -------------------------------")
            sh '/root/DSW_IM/exec.ksh'
            println("------------ tc_log_FVT_5 ------------------")
            sh 'cat /root/DSW_IM/tc_log_FVT_5.log'
            println("------------ tc_log_FVT_6 ------------------")
            sh 'cat /root/DSW_IM/tc_log_FVT_6.log'
            println("---------------------------------------------------------------------------------")
        }
    }
}
