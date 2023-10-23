package uk.gov.hmcts.idam.userprofilebridge.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper;

@ExtendWith(MockitoExtension.class)
class ListenerErrorHandlerTest {

    @InjectMocks
    ListenerErrorHandler underTest;

    @Test
    public void testHandleError() {
        // coverage only
        underTest.handleError(SpringWebClientHelper.exception(HttpStatus.NOT_FOUND, new RuntimeException()));
        underTest.handleError(new RuntimeException());
    }

}
