package com.redhat.openshift.devspaces;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

@WebFilter(urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR, DispatcherType.ASYNC})
public class Redirect404Filter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(httpResponse) {
            @Override
            public void sendError(int sc) throws IOException {
                if (sc == HttpServletResponse.SC_NOT_FOUND) {
                    super.setStatus(HttpServletResponse.SC_FOUND);
                    super.setHeader("Location", "/");
                } else {
                    super.sendError(sc);
                }
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                if (sc == HttpServletResponse.SC_NOT_FOUND) {
                    super.setStatus(HttpServletResponse.SC_FOUND);
                    super.setHeader("Location", "/");
                } else {
                    super.sendError(sc, msg);
                }
            }
            
            @Override
            public void setStatus(int sc) {
                 if (sc == HttpServletResponse.SC_NOT_FOUND) {
                    super.setStatus(HttpServletResponse.SC_FOUND);
                    super.setHeader("Location", "/");
                } else {
                    super.setStatus(sc);
                }
            }
        };

        chain.doFilter(request, responseWrapper);
        
        // Final check
        if (responseWrapper.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
             httpResponse.setStatus(HttpServletResponse.SC_FOUND);
             httpResponse.setHeader("Location", "/");
        }
    }
}