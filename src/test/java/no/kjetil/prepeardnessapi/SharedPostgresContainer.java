package no.kjetil.prepeardnessapi;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class SharedPostgresContainer extends PostgreSQLContainer<SharedPostgresContainer> {
    private static final String IMAGE = "postgres:16-alpine";
    private static final SharedPostgresContainer INSTANCE =
    new SharedPostgresContainer()
            .withDatabaseName("prepeardnessdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withCommand(
                    "-c", "fsync=off",
                    "-c", "synchronous_commit=off",
                    "-c", "full_page_writes=off",
                    "-c", "shared_buffers=16MB",
                    "-c", "max_connections=50")
            .withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw"));

    public SharedPostgresContainer() {
        super(DockerImageName.parse(IMAGE));
    }

    public static SharedPostgresContainer getInstance() {
        return INSTANCE;
    }

    @Override
    public void start() {
        if(!isRunning()) {
            super.start();
        }
    }
}
