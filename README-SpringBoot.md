#Needs to be updated!!!

#Spring Boot Quick Starter for the "Snooze" API as a Service Platform

This Spring Boot project template and guide can help you get your [Snooze](https://github.com/RocketPartners/rckt_snooze) powered REST API up and running in less than 5 minutes.

## Quick Start

1. Clone this repo
1. Add your JDBC connection information to src/main/resources/snooze.properties
    ```properties
    ########################################################################
    ## DATABASES 
    ########################################################################
    db.class=io.rcktapp.api.handler.sql.SqlDb
    db.name=db
    db.driver=YOUR_JDBC_DRIVER_HERE
    db.url=YOUR_JDBC_URL_HERE
    db.user=YOUR_JDBC_USER_HERE
    db.pass=YOUR_JDBC_PASSWORD_HERE
    db.poolMin=3
    db.poolMax=5
    ```  
1. From your repo root directory, run 'gradlew build bootRun'
1. Open your browser to http://localhost:8080/demo/helloworld/${NAME_OF_A_TABLE_IN_YOUR_DB}
1. See the main [Snooze Readme](https://github.com/RocketPartners/rckt_snooze#configuring-your-api) for more information.
1. Enjoy!


## Clone / Fork

1. Create a new repo in GitHub without a README -- **IMPORTANT: DO NOT CREATE DEFAULT README**
1. Clone the repo 
`git clone git@github.com:RocketPartners/rckt_snooze_spring.git {NEW_REPO_NAME_GOES_HERE}`
1. Rename the cloned repo to "upstream"
`git remote rename origin upstream`
1. Make your new repo the "origin" remote
`git remote add origin git@github.com:RocketPartners/{NEW_REPO_NAME_GOES_HERE}.git`
1. Push the clone to your new repo
`git push origin master`
1. Make your local master branch use origin as it's remote-tracking-branch
`git branch master -u origin/master`


## Using Spring Boot Profiles

Snooze lets you [supply config files](https://github.com/RocketPartners/rckt_snooze#configuring-your-api) that are numbered and/or named to match a Spring deployment target profile.
This makes it easy to keep dev/stage/prod config files in the same project/deployment package and simply change the runtime profile.

For example, if you supply both a snooze.properties and snooze-prod.properties, the settings in the prod file will override the settings in the main "snooze.properties"
file.  To set the runtime target to prod and load this extra file, add the following to your JVM args '-Dspring.profiles.active=prod'.  You can have as many 
deployment targets as you would like.  A typical project may have something like the following all in the same project:

 * snooze.properties (will always load)
 * snooze-dev.properties (will only load when profile is 'dev' and will overwrite values from snooze.properties)
 * snooze-stage.properties (will only load when profile is 'stage' and will overwrite values from snooze.properties)
 * snooze-prod.properties (will only load when profile is 'prod' and will overwrite values from snooze.properties)
 
 
## Keeping Passwords out of Config Files

If you want to keep your database passwords (or any other sensative info) out of your snoooze.properties config files, you can simply set an environment variable OR
VM system property using the relevant key.  For example you could add '-Ddb.pass=MY_PASSWORD' to your JVM launch configuration OR something like 'EXPORT db.pass=MY_PASSWORD'
to the top of the batch file you use to launch our yoru Spring Boot app. If you are running in AWS ElasticBeanstalk, you could pass the keys/values as Environment Properties.  

For example, after running 'gradle build' you could launch like this:  

```bash
java -Ddb.pass=MY_PASSWORD -jar build/libs/rckt_snooze_spring-0.0.1.jar
```



 
## AWS ECS / Docker

> NOTE: For many of the sections below I will refer to ***your-app-name***, which will be used in many places and can be whatever you choose, but you should be consistent and use the same value each time. Also, I'm using ***123456789*** for the AWS account id, you'll need to swap that for your account id. I'm also assuming ***us-east-1*** for the region.

#### Build and Push Docker Image
- Get and install [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Edit the `Dockerfile`
  ```
    LABEL maintainer="your@email.com"
    LABEL application="your-app-name"
    ...
    ARG JAR_FILE=build/libs/your-app-name.jar
  ```
- `gradle build -Papp=your-app-name`
- `docker build --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') -t your-app-name .`
- If you have multiple environments you will need to specify the Dockerfile to use
- `docker build --file PATH_TO_DOCKER_FILE --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') --build-arg SPRING_PROFILE=OPTIONAL_SPRING_PROFILE -t your-app-name .`
- You can type `docker images` to see a list of all your images. You should see your newly created image.
- In AWS go to ECR and create a new repository. Name it ***your-app-name***
- Click into your new repository and click the "View push commands" in the upper right corner. You should see commands that look similar to these and you should run them. *Note: AWS also lists the build command, but you already made the build, so no need to do it again.*
    - `$(aws ecr get-login --no-include-email --region us-east-1)`
    - `docker tag your-app-name:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/your-app-name:latest`
    - `docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/your-app-name:latest`
- In AWS refresh your repository and you should see the image you just pushed.   



#### Create a Task Definition
- Find the file `taskdef.json` and edit the following
  - family *(your-app-name)*
  - containerDefinitions
    - name *(your-app-name)*
    - image (app_name:latest) *Note: you will need to switch it back to @IMAGE_TAG_NAME@ later*
    - awslogs-group
      - If the log group you specified above does not exist create it.  
      - `aws logs create-log-group --log-group-name /ecs/your-app-name`  
- You can removed the **taskRoleArn** for now and add one back as needed, this is equivalent to an EC2 machine role and will be needed to give permissions to the task for AWS resources such as S3, dynamo, etc. [How to create a task role](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html#create_task_iam_policy_and_role)
- Optional update these values if needed
    - cpu, memory, container cpu, container memory, port mapping
- Run the following command to create the task definition 
    - `aws ecs register-task-definition --cli-input-json file://taskdef.json`
    - *NOTE: if you have trouble with this command check your CLI version. I had to [upgrade](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) to get this to work (my version was 1.16.76 at the time of writing this)*    

#### Create Target Group in an Application Load Balancer
 - Assuming a ALB is already setup, go to the "Target Group" section and click "Create target group"
 - You'll be creating two "Target Groups" so give the first one a name like "app-name-env-1"
 - Select "IP", "HTTP" and Port 80
 - Select the appropriate VPC
 - If you know the health check endpoint include it otherwise you can edit this later.
 - Add the target group to the load balancer
   - Find the ALB that you want to use for this Target group. 
   - Click the  "Listeners" tab.
   - Click "view/edit rules" on the listener (if you have more than one, then add this to https and make http redirect to https)
   - Add a new rule, set the path and forward to the first target group that you just created.
 - Create a second "Target Group" with the same settings and a name like "app-name-env-2"
 - Make note of the ARN for the first target group you created. It can be found under the "Description" tab

#### Create an AWS ECS Service
- Find the `create-service.json` file and edit the following
  - taskDefinition
    - *example: portal-api-stage:1*
  - cluster
  - targetGroupArn *(From the previous step above)*
  - containerName  *(use the app name)*
  - containerPort *(8080 or whatever port your docker container listens on)*
  - subnets *(Choose subnets in the same az as the ALB. For Lift use private f and private e subnets)*
  - securityGroups
- Things like min/max/desired counts and auto scaling rules can be modified later
- To create the service run the following. Be sure to change the service name argument before running this command
  - `aws ecs create-service --service-name my-service --cli-input-json file://create-service.json`
- After creating this service, you should see your task spin up. Verify that everything is working before continuing.  

#### Create a CodeDeploy application
 - Create an **Application**, enter name and choose Amazon ECS for compute platform
 - Create a **Deployment Group** and enter..
   - Name (use app name)
   - Service role (code-deploy-role)
   - ECS Cluster 
   - ECS service name
   - Load balancer,  choose the correct ALB and Target groups 1 & 2

#### Build a CodePipeline
 - Edit the **ContainerName** in the `appspec.yml` file to match the name used in the **containerName** field in the `create-service.json` file.
 - Edit the following lines in the `buildspec.yml` file
   - CONTAINER_NAME
   - *Optionally edit..*
        - REPOSITORY_URI
        - TASK_DEF_FILE
        - APP_SPEC_FILE
        - DOCKER_FILE
        - SPRING_PROFILE
 - Edit the `taskdef.json` file  
   - Change the **image** value to use the @IMAGE_TAG_NAME@ placeholder for the tag name.
   - *Example:* `123456789.dkr.ecr.us-east-1.amazonaws.com/your-app-name:@IMAGE_TAG_NAME@`
   - Optional: If you are going to need AWS resource permissions, now would be a good time to create a taskRole and add it to the file.
        1. In IAM, create new role, select **Elastic Container Service**, then select **Elastic Container Service Task**
        1. Add the **AmazonECSTaskExecutionRolePolicy** managed policy.
        1. Name it and save.
        1. Copy the ARN of the role you just created and add a **taskRoleArn** field above the **executionRoleArn** field. 
        1. *Example:* `"taskRoleArn": "arn:aws:iam::123456789:role/ecs-your-app-name"`
        - *FYI - If you start your role name with "ecs" you won't need to modify the "code-deploy-role" policies. Otherwise you might need to include the role in the PassRole policy*
 - Commit the file changes to the deploy branch you are using for this app. *Example: branch: deploy-your-app-name*
 - In CodePipeline, click **Create pipline**
    - Name it, use existing service role, custom location
    - Choose GitHub for source provider, Click **Connect to GitHub**
    - Choose the repository and branch.
    - Choose **AWS CodeBuild** and click **Create project**
      - Name it
      - Choose **Managed image**
      - Choose **Ubuntu** operating system
      - Choose **Java** runtime
      - Choose **openjdk-8** runtime version
      - Click the **Privileged** checkbox *(Needed to be able to build docker images)*
      - New or existing role
         - *Note: If you created a new role be sure to give the role the **ecr-codebuild-codepipeline** policy*
      - Choose **Use a buildspec file**
      - Enter Buildspec path. *Example: env/app_name/buildspec.yml*
      - Click **Continue to CodePipeline**
    - Choose **Amazon ECS (Blue/Green)** for the deploy provider
    - Choose the CodeDeploy application you created earlier
    - Choose the CodeDeploy deployment group you created earlier
    - Choose **BuildArtifact** and enter **build/taskdef.json** for the Amazon ECS task definition
    - Choose **BuildArtifact** and enter **env/app_name/appspec.yml** for the AWS CodeDeploy AppSpec file
    - Leave the "Dynamically update task definition image" fields empty and click **Next**
    - Click **Create pipeline**



#### Extras
- [ECS / ECR Code Deploy Tutorial](https://docs.aws.amazon.com/codepipeline/latest/userguide/tutorials-ecs-ecr-codedeploy.html)

## Docker Cheatsheet
 - docker build --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') --build-arg BUILD_VERSION=0.0.1 -t your-app-name .
 - docker run -p 8080:8080 your-app-name
 - docker images
 - docker ps -a
 - docker inspect your-app-name
 - docker rm
 - docker rmi
 - docker rmi $(docker images -f "dangling=true" -q)  