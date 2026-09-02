package com.pms.property;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.pms.common.security.CurrentUserProvider;
import com.pms.common.security.JwtAuthenticationFilter;

@SpringBootTest
class PropertyServiceApplicationTests {

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void contextLoads() {
    }
}

