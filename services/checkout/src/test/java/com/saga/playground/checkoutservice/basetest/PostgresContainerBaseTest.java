package com.saga.playground.checkoutservice.basetest;

import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
// for preventing transaction deletion between tests override each other
public abstract class PostgresContainerBaseTest {

    protected static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER =
        new PostgreSQLContainer<>("postgres:14.16-alpine3.20")
            .withDatabaseName("saga_playground")
            .withUsername("123")
            .withPassword("123")
            .withReuse(false);

    static {
        POSTGRE_SQL_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRE_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRE_SQL_CONTAINER::getPassword);
    }
}
