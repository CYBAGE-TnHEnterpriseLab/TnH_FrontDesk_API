package com.pms.housekeeping;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class HousekeepingApplicationTest {

    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            HousekeepingApplication.main(new String[]{"--test"});
            springApplication.verify(() -> SpringApplication.run(HousekeepingApplication.class, new String[]{"--test"}));
        }
    }
}

