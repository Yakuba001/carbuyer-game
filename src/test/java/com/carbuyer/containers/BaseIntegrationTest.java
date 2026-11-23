package com.carbuyer.containers;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest extends MainContainer {

    protected static int threadCount;

    @Autowired
    void initThreadCount(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            threadCount = hikariDataSource.getMaximumPoolSize();
        } else {
            threadCount = Runtime.getRuntime().availableProcessors();
        }
    }
}
