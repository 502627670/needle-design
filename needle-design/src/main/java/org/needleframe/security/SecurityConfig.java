package org.needleframe.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.core.MessageCode;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.utils.JsonUtils;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.ui.ModelMap;

@Configuration
@EnableAspectJAutoProxy
@EnableJpaAuditing
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	public final static String ADMIN_USER = "administrator";
	
	static DefaultSecurityContextService securityContextService = new DefaultSecurityContextService();
	
	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
	
	@Bean
    public UserDetailsService userDetailsService() {
    	return new UserDetailsServiceImpl();
    }
    
	@Bean
    public AuditorAware<String> auditor() {
		AuditorAware<String> auditorAware = new AuditorAware<String>() {
			public Optional<String> getCurrentAuditor() {
				SessionUser user =  (SessionUser) SecurityUtils.currentUser();
				String username = user == null ? null : user.getUsername();
				return Optional.ofNullable(username);
			}
		};
		return auditorAware;
    }
	
	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }
		
	@Override
	public void configure(WebSecurity webSecurity) throws Exception{
	    webSecurity.ignoring()
	    	.antMatchers("/")
	    	.antMatchers("/static/**")
	    	.antMatchers("/resources/**")
	    	.antMatchers("/css/**")
	    	.antMatchers("/libs/**");
	}
	
	@Override
    protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
			.requestMatchers(PathRequest.toStaticResources()
					.atCommonLocations()).permitAll()
			.antMatchers("/").permitAll()
			.antMatchers("/app.json").permitAll()
	        .antMatchers("/index.html").permitAll()
	        .antMatchers("/resources/**").permitAll()
	        .antMatchers("/static/**").permitAll()
	        .antMatchers("/css/**").permitAll()
	        .antMatchers("/libs/**").permitAll()
	        .antMatchers("/upload/**").permitAll()
	        .antMatchers("*.jpg").permitAll()
	        .antMatchers("*.jpeg").permitAll()
	        .antMatchers("*.png").permitAll()
	        .antMatchers("*.gif").permitAll();

		http.formLogin()
			.loginPage("/login")
			.permitAll()
			.usernameParameter("username")
	    	.passwordParameter("password")
	    	.loginProcessingUrl("/login")
	    	.successHandler(authenticationSuccessHandler())
	    	.failureHandler(authenticationFailureHandler())
	        .and()
	        .rememberMe().rememberMeParameter("rememberMe")
	        .and()
	        .userDetailsService(userDetailsService())
	        .logout()
	        .logoutUrl("/logout")
	        .permitAll()
	    	.invalidateHttpSession(true)
	    	.logoutSuccessHandler(logoutSuccessHandler());
	        
		http.sessionManagement()
	        .maximumSessions(1)
	        .expiredSessionStrategy(sessionInformationExpiredStrategy())
	        .and()
	        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
	        .sessionAuthenticationFailureHandler(authenticationFailureHandler())
	        .invalidSessionUrl("/");

		http.authorizeRequests()
	        .anyRequest()
	        .authenticated()
	        .and().cors()
	    	.and().csrf().disable()
	    	.exceptionHandling()
	    	.authenticationEntryPoint(authenticationEntryPoint())
	    	.accessDeniedHandler(accessDeniedHandler());
	    	
    }
	
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
    	AuthenticationSuccessHandler authenticationSuccessHandler = new AuthenticationSuccessHandler() {
			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
				Object principal = authentication.getPrincipal();
				String token = "";
				if(SessionUser.class.isAssignableFrom(principal.getClass())) {
					SessionUser  userDetails = (SessionUser) principal;
					if(userDetails.getAuthorities().isEmpty()) {
						token = userDetails.getId().toString();
					}
					else {
						token = userDetails.getAuthorities().iterator().next().toString();
					}
				}
				response.setContentType("application/json;charset=utf-8");
				response.setCharacterEncoding("UTF-8");
				try(PrintWriter out = response.getWriter()) {
					ModelMap data = new ModelMap();
		            data.addAttribute("token", token);
		            ResponseMessage responseMessage = ResponseMessage.success("登录成功").data(data);
		            out.write(JsonUtils.toJSON(responseMessage));
		            out.flush();
				}
	        }
    	};

    	return authenticationSuccessHandler;
    }
    
    public AuthenticationFailureHandler authenticationFailureHandler() {
    	AuthenticationFailureHandler authenticationFailureHandler = new AuthenticationFailureHandler() {
			public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
				response.setContentType("application/json;charset=utf-8");
				response.setCharacterEncoding("UTF-8");
				try(PrintWriter out = response.getWriter()) {
					ResponseMessage responseMessage = ResponseMessage.failed(exception.getMessage());
	                out.write(JsonUtils.toJSON(responseMessage));
	                out.flush();
				}
			}
    	};
    	return authenticationFailureHandler;
    }
    
    public AccessDeniedHandler accessDeniedHandler() {
    	AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandler() {
			@Override
			public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
				response.setContentType("application/json;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setCharacterEncoding("UTF-8");
				try(PrintWriter out = response.getWriter()) {
					ResponseMessage responseMessage = ResponseMessage.failed("特权不足");
	                out.write(JsonUtils.toJSON(responseMessage));
	                out.flush();
				}
			}
    	};
    	return accessDeniedHandler;
    }
    
    public LogoutSuccessHandler logoutSuccessHandler() {
    	LogoutSuccessHandler logoutSuccessHandler = new LogoutSuccessHandler() {
			public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
				response.setContentType("application/json;charset=utf-8");
				response.setCharacterEncoding("utf-8");
				try(PrintWriter out = response.getWriter()) {
					ResponseMessage responseMessage = ResponseMessage.success("登出成功");
	                out.write(JsonUtils.toJSON(responseMessage));
	                out.flush();
				}
			}
    	};
    	return logoutSuccessHandler;
    }
    
    public SessionInformationExpiredStrategy sessionInformationExpiredStrategy() {
    	return new SessionInformationExpiredStrategy() {
			public void onExpiredSessionDetected(SessionInformationExpiredEvent event)
					throws IOException, ServletException {
				event.getResponse().setCharacterEncoding("utf-8");
				try(PrintWriter out = event.getResponse().getWriter()) {
					ResponseMessage responseMessage = 
							ResponseMessage.failed(MessageCode.SESSION_EXPIRED, "会话已过期");
					out.write(JsonUtils.toJSON(responseMessage));
					out.flush();
				}
			}
    	};
    }
    
    public AuthenticationEntryPoint authenticationEntryPoint() {
    	return new AuthenticationEntryPoint() {
			public void commence(HttpServletRequest request, 
					HttpServletResponse response,
					AuthenticationException authException) throws IOException, ServletException {
				response.setCharacterEncoding("UTF-8");
				try(PrintWriter out = response.getWriter()) {
					ResponseMessage responseMessage = 
							ResponseMessage.failed(MessageCode.SESSION_EXPIRED, "会话已过期");
	                out.write(JsonUtils.toJSON(responseMessage));
	                out.flush();
				}
			}
    	};
    }
    
}
