package gov.cdc.izgateway.configuration;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.webjars.WebJarVersionLocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwaggerUiVersionContextTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SwaggerUiVersionConfig.class, SwaggerUiConfigPropertiesTestConfig.class);

    @Test
    void postProcessorAlignsSwaggerUiVersionInRealContext() {
        String expected = new WebJarVersionLocator().version(SwaggerUiVersionConfig.SWAGGER_UI_WEBJAR_NAME);
        assertNotNull(expected, "swagger-ui webjar must be on the test classpath");

        contextRunner.run(context -> {
            SwaggerUiConfigProperties props = context.getBean(SwaggerUiConfigProperties.class);
            assertEquals(expected, props.getVersion(),
                    "BeanPostProcessor must override SwaggerUiConfigProperties.version during context init");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class SwaggerUiConfigPropertiesTestConfig {
        @Bean
        SwaggerUiConfigProperties swaggerUiConfigProperties() {
            SwaggerUiConfigProperties props = new SwaggerUiConfigProperties();
            props.setVersion("0.0.0-test-pin");
            return props;
        }
    }
}
