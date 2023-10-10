package uk.gov.hmcts.idam.userprofilebridge.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationEntity;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationStatus;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationType;

import java.sql.Timestamp;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "scheduler.enabled", matchIfMissing = true)
public interface InvitationEntityRepository extends CrudRepository<InvitationEntity, String> {

    Slice<InvitationEntity> findByLastModifiedAfterAndInvitationStatusAndInvitationTypeInOrderByLastModifiedAsc(
        Timestamp createdSince,
        InvitationStatus invitationStatus,
        List<InvitationType> invitationTypes,
        Pageable pageable
    );

}
