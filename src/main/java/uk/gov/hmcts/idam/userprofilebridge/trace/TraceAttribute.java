package uk.gov.hmcts.idam.userprofilebridge.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;

public enum TraceAttribute implements AttributeKey<String> {

    USER_ID, EMAIL, SESSION_CLIENT_ID, SESSION_KEY, SESSION_ID,
    FORCE_DELETE, CLIENT_ID, ROLE_NAME, OUTCOME, COUNT, ROLE_NAMES, DESTINATION, CATEGORIES, ERROR,
    USER_PROFILE_STATE, CASEWORKER_PROFILE_STATE, EVENT_COUNT, EXCLUDED_CLIENT;

    @Override
    public String getKey() {
        return name();
    }

    @Override
    public AttributeType getType() {
        return AttributeType.STRING;
    }
}
