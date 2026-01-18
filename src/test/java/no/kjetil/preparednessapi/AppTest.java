package no.kjetil.preparednessapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
@SpringBootTest
@ContextConfiguration(initializers = DotenvTestInitializer.class)
public class AppTest extends PostgreSqlIntegrationSetup {
    /**
     * Rigourous Test :-)
     */
    @Test
    public void testApp() {
        assertTrue(true);
    }
}
