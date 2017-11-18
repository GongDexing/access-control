package club.ibook.security.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import club.ibook.security.init.RoleHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class AccessFilter implements Filter {

    @Autowired
    private RoleHelper roleHelper;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // String role = tokenDecode(httpRequest.getHeader("Access-Token"));
        String role = httpRequest.getHeader("role");
        String method = httpRequest.getMethod().toLowerCase();
        String mapping = httpRequest.getServletPath();
        if (!roleHelper.isHasRoleAccess(role, method, mapping)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "not authorized");
        } else {
            chain.doFilter(request, response);
        }
    }

    private String tokenDecode(String token) {
        String secret = "test";
        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void destroy() {

    }

}
