package no.kjetil.prepeardnessapi;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class SharedPostgresContainer extends PostgreSQLContainer<SharedPostgresContainer> {
    private static final String IMAGE = "postgres:16.11-alpine";
    private static SharedPostgresContainer INSTANCE;

    public SharedPostgresContainer() {
        super(DockerImageName.parse(IMAGE));
    }

    @SuppressWarnings("resource")
    public static SharedPostgresContainer getInstance() {
        if(INSTANCE == null) {
            
            INSTANCE = new SharedPostgresContainer()
            .withDatabaseName("preparednessdb")
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
        }
        return INSTANCE;
    }

    @Override
    public void start() {
        if(!isRunning()) {
            super.start();
        }
    }
}
