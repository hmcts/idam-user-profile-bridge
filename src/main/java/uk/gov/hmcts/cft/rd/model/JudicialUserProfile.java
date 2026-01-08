package uk.gov.hmcts.cft.rd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JudicialUserProfile {

    @JsonProperty("sidam_id")
    private String sidamId;

    @JsonProperty("object_id")
    private String objectId;

    @JsonProperty("email_id")
    private String email;

    @JsonProperty("known_as")
    private String knownAs;

    private String surname;

    @JsonProperty("personal_code")
    private String personalCode;

    @JsonProperty("active_flag")
    private Boolean activeFlag;

    @JsonProperty("deleted_flag")
    private Boolean deletedFlag;

    @JsonProperty("retirement_date")
    private String retirementDate;

}
