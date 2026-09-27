package free.cobol2java.java.spring;

import free.cobol2java.java.SqlWriteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Default Spring registration for the framework-neutral SQL write service. */
@Configuration(proxyBeanMethods = false)
public class SpringSqlWriteServiceConfiguration {
    @Bean
    public SqlWriteService sqlWriteService() {
        return new SqlWriteService();
    }
}
