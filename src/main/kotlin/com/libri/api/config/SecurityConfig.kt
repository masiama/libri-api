package com.libri.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
	@Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
	private val jwkSetUri: String,
	@Value("\${libri.cors.allowed-origins}")
	private val allowedOrigins: String
) {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.cors { it.configurationSource(corsConfigurationSource()) }
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers("/api/v1/ping").permitAll()
					.requestMatchers("/api/v1/internal/**").permitAll()
					.requestMatchers("/api/v1/images/**").permitAll()
					.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
					.anyRequest().authenticated()
			}
			.oauth2ResourceServer { oauth2 ->
				oauth2.jwt { it.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
			}

		return http.build()
	}

	@Bean
	fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
		val converter = JwtAuthenticationConverter()
		converter.setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
			val authorities = mutableListOf<SimpleGrantedAuthority>()

			if (jwt.getClaim<Boolean>("is_admin") == true) {
				authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
			}

			authorities
		}
		return converter
	}

	@Bean
	fun jwtDecoder(): JwtDecoder =
		NimbusJwtDecoder.withJwkSetUri(jwkSetUri).jwsAlgorithm(SignatureAlgorithm.RS256).build()

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = allowedOrigins.split(",")
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
		configuration.allowedHeaders = listOf("*")
		configuration.allowCredentials = true

		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/api/**", configuration)
		return source
	}
}
