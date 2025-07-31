package com.crescendo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


/**
 * Enables JPA repository for base packages that are defined under it.
 * The base packages are specified for the exact packages and these repositories are bind to the Entity manager factory
 * This tells where the repositories which extend JPA repository interfaces are
 * Entity Manager Factory are responsible for creation of Entity Manager Instances.
 * JPA uses these instances to interact with our database.
 * Transaction manager are responsible for binding to entity for transaction on different entities.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.crescendo.user_query",
                "com.crescendo.workflow_query"
        },
        entityManagerFactoryRef = "queryEntityManagerFactory",
        transactionManagerRef = "transactionManagerFactory"
)

public class QueryDBConfig {

        /**
         * Binds properties prefixed with "spring.datasource.query" in the application properties
         * @return a new Datasource property
         */
        @Primary
        @Bean(name = "queryDatasourceProperties")
        @ConfigurationProperties("spring.datasource.query")
        public DataSourceProperties queryDatasourceProperties(){
                return new DataSourceProperties();
        }

        /**
         * Builds an actual datasource(JDBC connection pool) using the datasource properties.
         * It will be used by the Entity Manager Factory
         */
        @Primary
        @Bean(name = "queryDataSource")
        public DataSource queryDataSource(){
                return queryDatasourceProperties()
                        .initializeDataSourceBuilder()
                        .build();
        }

        /**
         * Creates custom Entity Manager Factory for the data source
         * It tells JPA to only scan given packages for entity classes to associate with the given datasource
         * Required to isolate entity management for the multi-DB setup
         */
        @Primary
        @Bean(name = "queryEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean queryEntityManagerFactory(
                EntityManagerFactoryBuilder builder
        ){
                return builder
                        .dataSource(queryDataSource())
                        .packages(
                                "com.crescendo.user_query",
                                "com.crescendo.workflow_query"
                        )
                        .build();
        }

        /**
         * Defines Transaction Manager for the above Entity Manager Factory
         * This allows for transaction boundaries (@Transactional) to be handled correctly
         * "@Qualifier" ensures that the correct bean is injected.
         */
        @Primary
        @Bean(name = "queryTransactionManager")
        public PlatformTransactionManager queryTransactionManager(
                @Qualifier("queryEntityManagerFactory")
                LocalContainerEntityManagerFactoryBean queryEntityManagerFactory
        ){
            assert queryEntityManagerFactory.getObject() != null;
            return new JpaTransactionManager(queryEntityManagerFactory.getObject());
        }
}
