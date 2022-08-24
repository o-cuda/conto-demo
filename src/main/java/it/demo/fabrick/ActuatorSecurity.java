package it.demo.fabrick;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration(proxyBeanMethods = false)
public class ActuatorSecurity extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.requestMatcher(EndpointRequest.toAnyEndpoint()).authorizeRequests((requests) -> requests.anyRequest().permitAll());
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception 
//    {
//        auth.parentAuthenticationManager(authenticationManagerBean())
//            .userDetailsService(customUserDetailsService);
//    }

}

