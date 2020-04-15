dockerRun: startController
	## wait until it installs controller and ES
	## sleep 600
	## bash into the controller controller, change props to enable port 9200
	## docker exec controller /bin/bash -c "sed -i s/ad.es.node.http.enabled=false/ad.es.node.http.enabled=true/g events-service/processor/conf/events-service-api-store.properties"
	## restart ES to make the changes reflect
	## docker exec controller /bin/bash -c "pa/platform-admin/bin/platform-admin.sh submit-job --platform-name AppDynamicsPlatform --service events-service --job restart-cluster"
	## sleep 60
	## Run MA in docker.
	## start machine agent
	docker-compose --file docker-compose.yml up --force-recreate -d --build machine
	@echo started container ##################%%%%%%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&

startController:
	@echo starting container ##################%%%%%%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&
	## start controller
	docker-compose --file docker-compose.yml up --force-recreate -d --build controller

dockerStop:
	## stop and remove all containers
	sleep 60
	@echo remove containers and images
	## docker stop machine controller
	## docker rm machine controller
	docker-compose down --rmi all -v --remove-orphans
	docker rmi dtr.corp.appdynamics.com/appdynamics/machine-agent:4.5.18.2430
	docker rmi alpine
	@echo remove containers and images
	## always remove all unused networks, will cause a leak otherwise. use --force when running on TC
	docker network prune --force

sleep:
	@echo Waiting for 5 minutes to read the metrics
	sleep 300
	@echo Wait finished

terraformApply:
	@echo Download terraform
	# ${CURDIR}
	wget https://releases.hashicorp.com/terraform/0.12.20/terraform_0.12.20_linux_amd64.zip
	unzip -o terraform_0.12.20_linux_amd64.zip
	@echo Terraform downloaded
	# @echo Current Directory
	# ${CURDIR}
	sleep 60
	@echo Initialising terraform
	./terraform init
	## sudo terraform/terraform plan
	@echo Terraform initialised
	rm -rf terraform_0.12.20_linux_amd64.zip
	TF_VAR_CLIENT_ID="${CLIENT_ID}" TF_VAR_CLIENT_SECRET="${CLIENT_SECRET}" TF_VAR_SUBSCRIPTION_ID="${SUBSCRIPTION_ID}" TF_VAR_TENANT_ID="${TENANT_ID}" ./terraform apply -auto-approve
	@echo Terraform setup done

terraformDestroy:
	@echo Destroy instance
	TF_VAR_CLIENT_ID="${CLIENT_ID}" TF_VAR_CLIENT_SECRET="${CLIENT_SECRET}" TF_VAR_SUBSCRIPTION_ID="${SUBSCRIPTION_ID}" TF_VAR_TENANT_ID="${TENANT_ID}" ./terraform destroy -auto-approve
	@echo Instance destroyed
	rm -rf terraform
	@echo Terraform Removed