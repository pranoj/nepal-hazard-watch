package watch.nepalhazard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Single source of truth for allowed frontend origins, replacing the
 * per-controller @CrossOrigin annotations. Needed because the frontend is
 * reachable at more than one real origin (the bare domain and "www."), and
 * @CrossOrigin's origins attribute doesn't split a comma-separated property
 * value into multiple allowed origins.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
