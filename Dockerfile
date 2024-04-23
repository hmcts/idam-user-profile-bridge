# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.5.2
ARG PLATFORM=""
# Application image

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:21-distroless

ADD --chown=hmcts:hmcts build/libs/idam-user-profile-bridge.jar \
                        lib/applicationinsights.json /opt/app/

EXPOSE 8080
CMD [ "idam-user-profile-bridge.jar" ]
