package cl.duoc.tienda.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/** CORS corre primero: el preflight (OPTIONS) del navegador no lleva token. */
@Configuration
public class FiltrosConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(@Value("${cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(source));
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }

    @Bean
    FilterRegistrationBean<SeguridadFilter> seguridadFilter(JwtValidator jwtValidator) {
        FilterRegistrationBean<SeguridadFilter> registro = new FilterRegistrationBean<>(new SeguridadFilter(jwtValidator));
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registro;
    }
}
