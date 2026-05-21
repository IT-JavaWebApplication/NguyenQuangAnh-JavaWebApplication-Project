package com.smartacademic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Không khai báo SessionFactory bean riêng — Hibernate 6's SessionFactory
 * implement EntityManagerFactory nên sẽ gây circular dependency với JPA auto-config.
 * Hibernate Session được lấy qua HibernateSessionProvider khi cần.
 */
@Configuration
@EnableTransactionManagement
public class HibernateConfig {
}
