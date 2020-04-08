FROM alpine:latest
RUN apk add --no-cache openjdk8
WORKDIR /opt/appdynamics/machine-agent
COPY --from=dtr.corp.appdynamics.com/appdynamics/machine-agent /opt/appdynamics/machine-agent .

RUN apk update && apk add wget unzip

RUN sleep 60

ADD target/AzureCustomNamespaceMonitor-*.zip /opt/appdynamics/machine-agent/monitors

RUN unzip -q "/opt/appdynamics/machine-agent/monitors/AzureCustomNamespaceMonitor-*.zip" -d /opt/appdynamics/machine-agent/monitors
RUN find /opt/appdynamics/machine-agent/monitors -name '*.zip' -delete

COPY src/integration-test/resources/conf/config_ci.yml monitors/AzureCustomNamespaceMonitor/config.yml
# enable debug logging
RUN sed -i '1,/sigar/ s/info/debug/' conf/logging/log4j.xml


CMD ["sh", "-c", "java ${MACHINE_AGENT_PROPERTIES} -jar /opt/appdynamics/machine-agent/machineagent.jar"]