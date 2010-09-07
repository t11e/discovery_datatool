package com.t11e.discovery.datatool;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component("BypassAuthenticationFilter")
public class BypassAuthenticationFilter
  extends GenericFilterBean
{
  private static final List<GrantedAuthority> DEFAULT_ROLES =
    Arrays.asList((GrantedAuthority) new GrantedAuthorityImpl("ROLE_USER"));
  private final byte[] bypassLock = {};
  private boolean bypass = true;

  @Override
  public void doFilter(
    final ServletRequest request,
    final ServletResponse response,
    final FilterChain chain)
    throws IOException, ServletException
  {
    final boolean doBypass;
    synchronized (bypassLock)
    {
      doBypass = bypass;
    }
    if (doBypass && SecurityContextHolder.getContext().getAuthentication() == null)
    {
      final AnonymousAuthenticationToken auth =
        new AnonymousAuthenticationToken("bypass_auth", "bypass_auth", DEFAULT_ROLES);
      auth.setDetails(new User("bypass_auth", "bypass_auth", true, true, true, true, DEFAULT_ROLES));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    chain.doFilter(request, response);
  }

  public void setBypass(final boolean bypass)
  {
    synchronized (bypassLock)
    {
      this.bypass = bypass;
    }
  }
}
