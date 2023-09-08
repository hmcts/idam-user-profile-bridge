package uk.gov.hmcts.idam.userprofilebridge.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.cft.idam.api.v2.common.jpa.StringListConverter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity(name = "invitation")
public class InvitationEntity {

    @Column(name = "id")
    @NotEmpty
    @Id
    private String id;

    @Column(name = "invitationtype")
    @Enumerated(EnumType.STRING)
    private InvitationType invitationType;

    @Column(name = "invitationstatus")
    @Enumerated(EnumType.STRING)
    private InvitationStatus invitationStatus;

    @Column(name = "activationtoken")
    private String activationToken;

    @Column(name = "userid")
    @NotEmpty
    private String userId;

    @Column(name = "email")
    @NotEmpty
    private String email;

    @Transient
    private String forename;

    @Transient
    private String surname;

    @Column(name = "activationrolenames")
    @Convert(converter = StringListConverter.class)
    private List<String> activationRoleNames;

    @Column(name = "clientid")
    private String clientId;

    @Column(name = "successredirect")
    private String successRedirect;

    @Column(name = "invitedby")
    private String invitedBy;

    @Column(name = "createdate")
    private Timestamp createDate;

    @Column(name = "lastmodified")
    private Timestamp lastModified;

}
