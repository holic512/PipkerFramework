package com.pipker.server;

import com.pipker.business.common.api.CommonApiCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pipker.dev.route-manifest.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DevRouteManifestTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enabledManifestReadsLatestDatabaseRoutesWithoutSensitiveFields() throws Exception {
        mockMvc.perform(get("/api/_dev/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.routes[0].componentKey").value("system/overview/index"))
                .andExpect(jsonPath("$.data.routes[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.routes[0].accessToken").doesNotExist());

        jdbcTemplate.update("UPDATE system_menu SET route_path = '/system/overview-live' WHERE route_name = 'SystemOverview'");

        mockMvc.perform(get("/api/_dev/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routes[0].path").value("/system/overview-live"));
    }
}
