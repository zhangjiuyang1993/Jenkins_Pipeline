# Jenkins_Pipeline

## Flow
![](./IMG/Auto_Deploy.png)

## Files Description
* **pipeline_stable.groovy**: Main file - define the jenkins job flow
* **Jenkisnfile**: define the jenkins job flow, used by SCM
* **jq_parse_component_process_json.ksh**: Catch Log - Function: parse the Json file 
* <floder> **resource**: some cofigure and script about check code from RTC 

## Resource Folder
* **attribute.txt**: map the category && environment in WI
![](./IMG/WI_category_env.png)
* **loadfiles.bat**: check code from RTC by SCM tool

## Jar File
* **Function**: query && describe the WI's description
* **Location**: ${Pipeline's workspace}/FetchFiles.jar

## Version Folder
![](./IMG/version_folder.png)

## Version Repo(UAT) 
![](./IMG/version_repo.png)

## WI Description
![](./IMG/WI_Description.png)

## **Configure:**
### Jenkins
* **UCD server**:
![](./IMG/jenkins_ucd.png)
* **params**:
* **RepositoryURI**: RTC
* **user**: login in RTC
* **Password**: for user
* **ProjectAreaName**: ex DSC PROD
* **WorkItemID**: ex 1169311

### Urbancode Deploy
* **Component Configure**:
![](./IMG/ucd.png)
* **Application**: ex, DSW_IM_DevOps_Applications(now)
* **Environment**: defined by WI
* **Component**: defined by WI
* **Process**: defined by WI

## **Usage**:
### Start the Job - FVT && UAT
* **step1**
![](./IMG/start_job.jpg)
* **step2**
![](./IMG/build_start.jpg)
* **step3**
![](./IMG/provide_args.jpg)

### UAT
![](./IMG/UAT_Script.png)
