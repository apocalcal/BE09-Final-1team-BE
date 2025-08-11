package com.newnormallist.adminservice.client;

import com.newnormallist.adminservice.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
        name = "user-service",
        // url = "${user-service.url:http://localhost:8080}",
        configuration = FeignAuthConfig.class
)
public interface UserAdminClient {
    @DeleteMapping("/users/internal/admin/{userId}")
    void hardDelete(@PathVariable("id") Long userId);

    @DeleteMapping("/users/internal/admin/batch")
    Map<String, Object> purge(@RequestParam("before") String beforeIso);

}
