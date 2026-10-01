package Policy_Management.Policy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "security.jwt.secret=pms-auth-super-secret-key-change-me-in-prod-2026")
@SpringBootTest
class PolicyApplicationTests {

	@Test
	void contextLoads() {
	}

}
