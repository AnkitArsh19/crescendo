package com.crescendo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;


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
                "com.crescendo.user.user_query",
                "com.crescendo.workflow.workflow_query",
                "com.crescendo.steps.steps_query",
                "com.crescendo.connections.connections_query",
                "com.crescendo.emailservice.apikey.key_query",
                "com.crescendo.emailservice.emailtemplate.template_query",
                "com.crescendo.app"
        },
        entityManagerFactoryRef = "queryEntityManagerFactory",
        transactionManagerRef = "queryTransactionManager"
)

public class QueryDBConfig {

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
         * Binds properties prefixed with "spring.datasource.query" in the application properties
         * @return a new Datasource property
         */
        @Bean(name = "queryDatasourceProperties")
        @ConfigurationProperties("spring.datasource.query")
        public DataSourceProperties queryDatasourceProperties(){
                return new DataSourceProperties();
        }

        /**
         * Builds an actual datasource(JDBC connection pool) using the datasource properties.
         * It will be used by the Entity Manager Factory
         */
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
        @Bean(name = "queryEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean queryEntityManagerFactory(
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
                        .dataSource(queryDataSource())
                        .packages(
                                "com.crescendo.user.user_query",
                                "com.crescendo.workflow.workflow_query",
                                "com.crescendo.steps.steps_query",
                                "com.crescendo.connections.connections_query",
                                "com.crescendo.emailservice.apikey.key_query",
                                "com.crescendo.emailservice.emailtemplate.template_query",
                                "com.crescendo.emailservice.email_log",
                                "com.crescendo.app",
                                "com.crescendo.publicapi.audit",
                                "com.crescendo.publicapi.oauth",
                                "com.crescendo.publicapi.oauth.persistence"
                        )
                        .persistenceUnit("query")
                        .properties(props)
                        .build();
        }

        /**
         * Defines Transaction Manager for the above Entity Manager Factory
         * This allows for transaction boundaries (@Transactional) to be handled correctly
         * "@Qualifier" ensures that the correct bean is injected.
         */
        @Bean(name = "queryTransactionManager")
        public PlatformTransactionManager queryTransactionManager(
                @Qualifier("queryEntityManagerFactory")
                LocalContainerEntityManagerFactoryBean queryEntityManagerFactory
        ){
            assert queryEntityManagerFactory.getObject() != null;
            return new JpaTransactionManager(queryEntityManagerFactory.getObject());
        }
}
