package com.aeroops;

import com.aeroops.config.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class ApiApplicationTests {

    @Test
    void contextLoads() {
        // Fails if wiring (security, JPA, Flyway migrations) is broken.
    }
}
