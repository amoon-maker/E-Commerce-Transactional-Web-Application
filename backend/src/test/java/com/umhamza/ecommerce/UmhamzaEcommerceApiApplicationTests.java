package com.umhamza.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"DB_URL=jdbc:h2:mem:application-context-test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS dbo",
		"DB_USERNAME=sa",
		"DB_PASSWORD=",
		"APP_DEV_PASSWORD=context-test-password",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class UmhamzaEcommerceApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
