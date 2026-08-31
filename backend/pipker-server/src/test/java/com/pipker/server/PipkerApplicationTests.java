package com.pipker.server;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.LoginType;
import com.pipker.server.controller.PingController;
import com.pipker.starter.satoken.service.AuthSessionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "pipker.security.auth.public-routes[0]=GET /api/ping",
        "pipker.security.auth.public-routes[1]=POST /api/test/login"
})
@AutoConfigureMockMvc
@Import(PipkerApplicationTests.TestApiConfiguration.class)
class PipkerApplicationTests {

    private static String testAesKey;

    @Autowired
    private PingController pingController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaTokenDao saTokenDao;

    @BeforeAll
    static void createRuntimeOnlyTestKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        testAesKey = Base64.getEncoder().encodeToString(bytes);
    }

    @DynamicPropertySource
    static void configureSecurity(DynamicPropertyRegistry registry) {
        registry.add("pipker.security.crypto.encryption.aes-gcm-key", () -> testAesKey);
    }

    @Test
    void pingReturnsServerStatus() {
        assertThat(pingController.ping()).isEqualTo("Pipker Server is running.");
    }

    @Test
    void memoryStoreIsTheDefaultSaTokenDao() {
        assertThat(saTokenDao).isInstanceOf(SaTokenDaoDefaultImpl.class);
    }

    @Test
    void pingIsPublicButProtectedApiReturnsProblemDetailWithoutToken() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pipker Server is running."));

        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void bearerTokenAllowsProtectedApiAndResolvesGenericIdentity() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/test/login"))
                .andExpect(status().isOk())
                .andReturn();
        String token = loginResult.getResponse().getContentAsString();

        mockMvc.perform(get("/api/test/protected").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestApiConfiguration {

        @Bean
        TestAuthController testAuthController(AuthSessionService authSessionService) {
            return new TestAuthController(authSessionService);
        }
    }

    @RestController
    static class TestAuthController {

        private final AuthSessionService authSessionService;

        TestAuthController(AuthSessionService authSessionService) {
            this.authSessionService = authSessionService;
        }

        @PostMapping("/api/test/login")
        String login() {
            return authSessionService.login(new LoginIdentity(new LoginType("member"), "42")).value();
        }

        @GetMapping("/api/test/protected")
        String protectedEndpoint() {
            return authSessionService.currentIdentity().orElseThrow().userId();
        }
    }
}
