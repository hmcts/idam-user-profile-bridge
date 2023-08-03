# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.4.15
ARG PLATFORM=""
# Application image

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/idam-user-profile-bridge.jar /opt/app/

EXPOSE 8080
CMD [ "idam-user-profile-bridge.jar" ]
