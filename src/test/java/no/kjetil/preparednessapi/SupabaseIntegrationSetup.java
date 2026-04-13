package no.kjetil.preparednessapi;

import no.kjetil.preparednessapi.utils.DotenvLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class SupabaseIntegrationSetup {

	private static String jdbcUrl;
	private static String dbUser;
	private static String dbPassword;

	@BeforeAll
	static void beforeAll() throws Exception {
		// Load DATABASE_URL from .env or environment
		String databaseUrl = DotenvLoader.getOrDefault("DATABASE_URL", System.getenv("DATABASE_URL"));
		if (databaseUrl == null || databaseUrl.isBlank()) {
			// nothing to test against; skip — but fail fast to make intent explicit
			throw new IllegalStateException("DATABASE_URL not set for Supabase integration tests");
		}

		// If already a JDBC url, use as-is
		if (databaseUrl.startsWith("jdbc:")) {
			jdbcUrl = databaseUrl;
			// username/password may not be present; rely on properties from env if available
			dbUser = System.getenv("DB_USER");
			dbPassword = System.getenv("DB_PASSWORD");
		} else {
			// Parse postgres URI like: postgresql://user:pass@host:port/dbname
			URI uri = new URI(databaseUrl);
			String userInfo = uri.getUserInfo();
			if (userInfo != null && userInfo.contains(":")) {
				String[] up = userInfo.split(":", 2);
				dbUser = up[0];
				dbPassword = up[1];
			}
			String host = uri.getHost();
			int port = uri.getPort() == -1 ? 5432 : uri.getPort();
			String path = uri.getPath();
			String dbName = (path != null && path.length() > 1) ? path.substring(1) : "postgres";
			// ensure SSL when connecting to Supabase
			jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require", host, port, dbName);
		}

		// Verify we can open a connection
		try (Connection c = (dbUser != null)
				? DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)
				: DriverManager.getConnection(jdbcUrl)) {
			assertThat("Database connection should be valid", c.isValid(5), is(true));
		}
	}

	@AfterAll
	static void afterAll() {
		// no-op for now
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> jdbcUrl);
		registry.add("spring.datasource.username", () -> dbUser);
		registry.add("spring.datasource.password", () -> dbPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}
}
