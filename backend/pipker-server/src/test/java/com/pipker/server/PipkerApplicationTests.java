package com.pipker.server;

import com.pipker.server.controller.PingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PipkerApplicationTests {

    @Autowired
    private PingController pingController;

    @Test
    void pingReturnsServerStatus() {
        assertThat(pingController.ping()).isEqualTo("Pipker Server is running.");
    }
}
