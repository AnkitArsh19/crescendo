package com.crescendo.config;

import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Defines a reusable EntityManagerFactoryBuilder bean that knows how to configure Hibernate properties.
 * Gives central control over JPA and Hibernate settings, such as DDL generation, dialect, and SQL formatting.
 */
@Configuration
public class JpaConfig {

    @Bean
    /// This creates a shared EntityManagerFactoryBuilder that can be used in both CommandConfig and QueryConfig.
    /// This builder is used to construct a Local EntityManagerFactory for the application.
    /// It helps attach the datasource, entity packages, and JPA properties.
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {

        /// Tells spring to use Hibernate as the JPA implementation(vendor)
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        /// Enables DDL generation, which allows Hibernate to create/update the database schema
        vendorAdapter.setGenerateDdl(true);

        /// This is a factory function that accepts a datasource and returns a map of JPA properties.
        /// It is a way of saying I want Hibernate to use these specific settings for the JPA context no matter what datasource is used
        Function<DataSource, Map<String, ?>> jpaPropertiesFactory = dataSource -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("hibernate.show_sql", false);
            properties.put("hibernate.format_sql", true);
            return properties;
        };
        
        return new EntityManagerFactoryBuilder(vendorAdapter, jpaPropertiesFactory, null);
    }
}
