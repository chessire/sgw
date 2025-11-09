package sgw.common.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import sgw.common.web.condition.ConditionalOnPropertySimple;

@Configuration
@EnableWebSecurity
@ConditionalOnPropertySimple(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .anyRequest().authenticated()
            .and()
            .httpBasic();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(inMemoryUserDetailsService());
    }

    @Bean
    public UserDetailsService inMemoryUserDetailsService() {
        UserDetails user = User.withUsername("admin")
            .password("admin")
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}

