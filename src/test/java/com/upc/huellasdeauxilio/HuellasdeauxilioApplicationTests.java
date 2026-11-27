package com.upc.huellasdeauxilio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Contexto aislado de PostgreSQL local, RDS, SMTP y configuracion personal.
 * La clave es ficticia y solo se utiliza durante las pruebas automatizadas.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:huellas_context_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never",
        "jwt.secret=SFVFTExBU19ERV9BVVhJTElPX1VOSVRfVEVTVF9LRVlfTk9UX0ZPUl9QUk9EVUNUSU9OXzIwMjZfX19fX1hIVUVMTEFTX0RFX0FVWElMSU9fVU5JVF9URVNUX0tFWV9OT1RfRk9SX1BST0RVQ1RJT05fMjAyNl9fX19fWA=="
})
class HuellasdeauxilioApplicationTests {

    @Test
    void contextLoads() {
    }
}
