package com.skyline.org.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class FlywayMigrationConfig {

    public static final String MIGRATION_BEAN_NAME = "databaseSchemaMigration";

    @Bean(name = MIGRATION_BEAN_NAME)
    public Object databaseSchemaMigration(DataSource dataSource, Environment environment) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        if (environment.getProperty("app.flyway.repair-on-migrate", Boolean.class, false)) {
            flyway.repair();
        }
        flyway.migrate();
        return new Object();
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnMigration() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition definition = beanFactory.getBeanDefinition("entityManagerFactory");
                definition.setDependsOn(MIGRATION_BEAN_NAME);
            }
        };
    }
}
