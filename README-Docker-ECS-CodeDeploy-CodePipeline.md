#Needs to be updated!!!

 
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