package no.kjetil.preparednessapi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
@Disabled
@SpringBootTest
@ContextConfiguration(initializers = DotenvTestInitializer.class)
public class AppTest extends PostgreSqlIntegrationSetup {
    /**
     * Rigourous Test :-)
     */
    @Disabled
    @Test
    public void testApp() {
        assertTrue(true);
    }
}
