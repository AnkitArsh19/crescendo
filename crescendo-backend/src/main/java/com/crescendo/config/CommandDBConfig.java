package com.crescendo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

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
                "com.crescendo.auth.token.email",
                "com.crescendo.auth.token.password",
                "com.crescendo.emailservice.domain",
                "com.crescendo.user.user_command",
                "com.crescendo.storage.storage_command",
                "com.crescendo.workflow.workflow_command",
                "com.crescendo.steps.steps_command",
                "com.crescendo.steps.step_condition",
                "com.crescendo.connections.connections_command",
                "com.crescendo.logbook.step_run",
                "com.crescendo.logbook.workflow_run",
                "com.crescendo.emailservice.apikey.key_command",
                "com.crescendo.emailservice.email_log",
                "com.crescendo.emailservice.emailtemplate.template_command",
                "com.crescendo.emailservice.audience",
                "com.crescendo.emailservice.broadcast",
                "com.crescendo.emailservice.suppression",
                "com.crescendo.webhook",
                "com.crescendo.security.mfa",
                "com.crescendo.admin",
                "com.crescendo.settings.oauth",
                "com.crescendo.logbook.outbox",
                "com.crescendo.publicapi.audit",
                "com.crescendo.publicapi.oauth",
                "com.crescendo.publicapi.oauth.persistence",
                "com.crescendo.execution.suspension",
                "com.crescendo.emailservice.customevent",
                "com.crescendo.emailservice.dmarc",
                "com.crescendo.emailservice.outboundwebhook"
        },
        entityManagerFactoryRef = "commandEntityManagerFactory",
        transactionManagerRef = "commandTransactionManager"
)
public class CommandDBConfig {

        /// These JPA/Hibernate properties are read from application.properties and applied
        /// explicitly because Spring Boot's auto-configuration is bypassed in a multi-datasource
        /// setup — global spring.jpa.* properties do NOT reach custom EntityManagerFactory beans.
        @Value("${spring.jpa.hibernate.ddl-auto:none}")
        private String ddlAuto;

        @Value("${spring.jpa.show-sql:false}")
        private boolean showSql;

        @Value("${spring.jpa.properties.hibernate.format_sql:false}")
        private boolean formatSql;

        @Value("${spring.jpa.properties.hibernate.jdbc.time_zone:UTC}")
        private String timeZone;

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
                /// Hibernate properties must be passed manually — spring.jpa.* is ignored
                /// by custom EntityManagerFactory beans in a multi-datasource setup.
                HashMap<String, Object> props = new HashMap<>();
                props.put("hibernate.hbm2ddl.auto", ddlAuto);
                props.put("hibernate.show_sql", showSql);
                props.put("hibernate.format_sql", formatSql);
                props.put("hibernate.jdbc.time_zone", timeZone);

                return builder
                        .dataSource(commandDataSource())
                        .packages(
                                "com.crescendo.user.user_command",
                                "com.crescendo.storage.storage_command",
                                "com.crescendo.workflow.workflow_command",
                                "com.crescendo.steps.steps_command",
                                "com.crescendo.steps.step_condition",
                                "com.crescendo.connections.connections_command",
                                "com.crescendo.logbook.step_run",
                                "com.crescendo.logbook.workflow_run",
                                "com.crescendo.emailservice.apikey.key_command",
                                "com.crescendo.emailservice.email_log",
                                "com.crescendo.emailservice.emailtemplate.template_command",
                                "com.crescendo.emailservice.audience",
                                "com.crescendo.emailservice.broadcast",
                                "com.crescendo.emailservice.suppression",
                                "com.crescendo.webhook",
                                "com.crescendo.security.mfa",
                                "com.crescendo.auth.token.email",
                                "com.crescendo.auth.token.password",
                                "com.crescendo.emailservice.domain",
                                "com.crescendo.settings.oauth",
                                "com.crescendo.admin",
                                "com.crescendo.logbook.outbox",
                                "com.crescendo.publicapi.audit",
                                "com.crescendo.publicapi.oauth",
                                "com.crescendo.publicapi.oauth.persistence",
                                "com.crescendo.execution.suspension",
                                "com.crescendo.emailservice.customevent",
                                "com.crescendo.emailservice.dmarc",
                                "com.crescendo.emailservice.outboundwebhook"
                        )
                        .properties(props)
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
