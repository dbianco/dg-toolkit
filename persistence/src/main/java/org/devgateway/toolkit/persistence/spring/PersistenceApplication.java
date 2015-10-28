/*******************************************************************************
 * Copyright (c) 2015 Development Gateway, Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/MIT
 *
 * Contributors:
 * Development Gateway - initial API and implementation
 *******************************************************************************/
package org.devgateway.toolkit.persistence.spring;

import org.devgateway.toolkit.persistence.dao.GenericPersistable;
import org.devgateway.toolkit.persistence.repository.RoleRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Run this application only when you need access to Spring Data JPA but without Wicket frontend
 * @author mpostelnicu
 *
 */
@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = RoleRepository.class)
@EnableTransactionManagement
@EntityScan(basePackageClasses = GenericPersistable.class)
@ComponentScan("org.devgateway.toolkit")
public class PersistenceApplication {
	

    public static void main(String[] args) {
        SpringApplication.run(PersistenceApplication.class, args);
    }
}