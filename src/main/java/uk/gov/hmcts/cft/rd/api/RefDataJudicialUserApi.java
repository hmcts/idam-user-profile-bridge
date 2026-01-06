package uk.gov.hmcts.cft.rd.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.cft.rd.model.JudicialUserProfile;

import java.util.List;
import java.util.Map;

@FeignClient(name = "rdjudicialapi", url = "${rd.judicial.api.url}")
public interface RefDataJudicialUserApi {

    String IDAM_IDS = "sidam_ids";
    String OBJECT_IDS = "object_ids";

    default JudicialUserProfile getUserByIdamId(String idamId) {
        return refreshUser(Map.of(IDAM_IDS, List.of(idamId)));
    }

    default JudicialUserProfile getUserByObjectId(String ssoId) {
        return refreshUser(Map.of(OBJECT_IDS, List.of(ssoId)));
    }

    @PostMapping
    JudicialUserProfile refreshUser(@RequestBody Map<String, List<String>> body);

}
