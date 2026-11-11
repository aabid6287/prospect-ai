package com.prospectai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class ProspectAiApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
