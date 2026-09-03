package com.pipker.server;

import com.pipker.starter.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pipker.file.enabled=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FileStorageDisabledMvcTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void disabledFileFeatureRegistersNoServiceAndNoPublicResourceMapping() throws Exception {
        assertThat(applicationContext.getBeansOfType(FileStorageService.class)).isEmpty();

        mockMvc.perform(get("/files/2026/09/04/missing.txt"))
                .andExpect(status().isNotFound());
    }
}
