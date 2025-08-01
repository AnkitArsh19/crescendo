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
 * Entity Manager Factory are responsible for creation of Entity Manager Instances.
 * JPA uses these instances to interact with our database.
 * Transaction manager are responsible for binding to entity for transaction on different entities.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.crescendo.user.user_command",
                "com.crescendo.workflow.workflow_command",
                "com.crescendo.steps.steps_command",
                "com.crescendo.connections.connections_command",
                "com.crescendo.logbook.StepRun",
                "com.crescendo.logbook.WorkflowRun",
                "com.crescendo.emailservice.apikey.key_command",
                "com.crescendo.emailservice.email_log",
                "com.crescendo.emailservice.emailtemplate.template_command",
                "com.crescendo.webhook"

        },
        entityManagerFactoryRef = "commandEntityManagerFactory",
        transactionManagerRef = "transactionManagerFactory"
)
public class CommandDBConfig {

        /**
         * Binds properties prefixed with "spring.datasource.command" in the application properties
         * @return a new Datasource property
         */
        @Primary
        @Bean(name = "commandDatasourceProperties")
        @ConfigurationProperties("spring.datasource.command")
        public DataSourceProperties commandDatasourceProperties(){
                return new DataSourceProperties();
        }

        /**
         * Builds an actual datasource(JDBC connection pool) using the datasource properties.
         * It will be used by the Entity Manager Factory
         */
        @Primary
        @Bean(name = "commandDataSource")
        public DataSource commandDataSource(){
                return commandDatasourceProperties()
                        .initializeDataSourceBuilder()
                        .build();
        }

        /**
         * Creates custom Entity Manager Factory for the data source
         * It tells JPA to only scan given packages for entity classes to associate with the given datasource
         * Required to isolate entity management for the multi-DB setup
         */
        @Primary
        @Bean(name = "commandEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean commandEntityManagerFactory(
                EntityManagerFactoryBuilder builder
        ){
                return builder
                        .dataSource(commandDataSource())
                        .packages(
                                "com.crescendo.user.user_command",
                                "com.crescendo.workflow.workflow_command",
                                "com.crescendo.steps.steps_command",
                                "com.crescendo.connections.connections_command",
                                "com.crescendo.logbook.StepRun",
                                "com.crescendo.logbook.WorkflowRun",
                                "com.crescendo.emailservice.apikey.key_command",
                                "com.crescendo.emailservice.email_log",
                                "com.crescendo.emailservice.emailtemplate.template_command",
                                "com.crescendo.webhook"
                        )
                        .build();
        }

        /**
         * Defines Transaction Manager for the above Entity Manager Factory
         * This allows for transaction boundaries (@Transactional) to be handled correctly
         * "@Qualifier" ensures that the correct bean is injected.
         */
        @Primary
        @Bean(name = "commandTransactionManager")
        public PlatformTransactionManager commandTransactionManager(
                @Qualifier("commandEntityManagerFactory")
                LocalContainerEntityManagerFactoryBean commandEntityManagerFactory
        ){
                assert commandEntityManagerFactory.getObject() != null;
                return new JpaTransactionManager(commandEntityManagerFactory.getObject());
        }
}
