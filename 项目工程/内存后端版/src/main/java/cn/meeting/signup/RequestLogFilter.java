package cn.meeting.signup;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLogFilter extends OncePerRequestFilter {
    private static final Logger log=LoggerFactory.getLogger(RequestLogFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String id=UUID.randomUUID().toString();request.setAttribute("requestId",id);response.setHeader("X-Request-Id",id);
        long began=System.nanoTime();
        try{chain.doFilter(request,response);}
        finally{if(request.getRequestURI().startsWith("/api/"))log.info("requestId={} {} {} status={} elapsedMs={}",
                id,request.getMethod(),request.getRequestURI(),response.getStatus(),(System.nanoTime()-began)/1_000_000);}
    }
}
