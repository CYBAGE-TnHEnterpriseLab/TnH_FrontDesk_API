package Policy_Management.Policy.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestUserContextAndProviderTest {

    private final RequestCurrentUserProvider provider = new RequestCurrentUserProvider();

    @AfterEach
    void clear() {
        RequestUserContext.clear();
    }

    @Test
    void requestUserContext_setGetClear_behavesAsExpected() {
        RequestUserContext.setUsername("user-a");
        assertEquals("user-a", RequestUserContext.getUsername());

        RequestUserContext.clear();
        assertNull(RequestUserContext.getUsername());
    }

    @Test
    void provider_returnsUsername_whenPresent() {
        RequestUserContext.setUsername("admin");

        assertEquals("admin", provider.getCurrentUsername());
    }

    @Test
    void provider_throws_whenMissingOrBlank() {
        assertThrows(IllegalStateException.class, provider::getCurrentUsername);

        RequestUserContext.setUsername(" ");
        assertThrows(IllegalStateException.class, provider::getCurrentUsername);
    }
}
