package com.example.chatvideo.security.jwt;

import com.example.chatvideo.security.services.UserDetailsService;
import io.micrometer.common.lang.NonNullApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter in the middle of Spring Security filters chain.
 * JwtFilter extends OncePerRequestFilter class to guarantee a single execution per request
 */
@Slf4j
@NonNullApi
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    /**
     * Constructor for JwtFilter class
     *
     * @param jwtService JwtService
     */
    public JwtFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Tells Spring if the user is authenticated, and continue the downstream filters.
     *
     * @param request     HttpServletRequest
     * @param response    HttpServletResponse
     * @param filterChain FilterChain
     * @throws ServletException ServletException
     * @throws IOException      IOException
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException, IOException {

        // If no Bearer token in Authorization header, continue the filter chain without updating authentication context
        if (!hasAuthorizationBearer(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getAccessToken(request);
        // If the token is invalid, continue the filter chain without updating authentication context
        if (!jwtService.validateAccessToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If the token valid, update the authentication context with the user details ID and email
        setAuthenticationContext(token, request);
        filterChain.doFilter(request, response);
    }

    /**
     * Check if request has a Bearer token in Authorization header
     *
     * @param request HttpServletRequest
     * @return boolean
     */
    private boolean hasAuthorizationBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(header)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^Bearer .+");
        Matcher matcher = pattern.matcher(header);
        if (matcher.find()) {
            return true;
        }
        log.error("Invalid Authorization header : " + "\"" + header + "\"");
        return false;
    }

    /**
     * Return an Access Token, read from Authorization header
     *
     * @param request HttpServletRequest
     * @return token
     */
    private String getAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header.split(" ", 2)[1].trim();
    }

    /**
     * Update the authentication context with the user details ID and email.
     *
     * @param token   Access Token
     * @param request HttpServletRequest
     */
    private void setAuthenticationContext(String token, HttpServletRequest request) {
        UserDetails userDetails = getUserDetails(token);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                token,
                userDetails.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    /**
     * @param token Access Token
     * @return UserDetails
     */
    private UserDetails getUserDetails(String token) {
        Jwt jwt = jwtService.readJwt(token);
        return userDetailsService.loadUserByUsername(jwt.getSubject());
    }

}
