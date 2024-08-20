package uk.gov.hmcts.cft.idam.api.v2.common;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;

@FeignClient(name = "idamv2usermanagement", url = "${idam.api.url}")
public interface IdamV2UserManagementApi {

    @GetMapping("/api/v2/users/{userId}")
    User getUser(@PathVariable String userId);

}
