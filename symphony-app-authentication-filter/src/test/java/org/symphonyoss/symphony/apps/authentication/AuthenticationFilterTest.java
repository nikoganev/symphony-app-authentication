package org.symphonyoss.symphony.apps.authentication;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.symphonyoss.symphony.apps.authentication.AuthenticationFilter
    .USER_INFO_ATTRIBUTE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.symphonyoss.symphony.apps.authentication.AuthenticationFilter;
import org.symphonyoss.symphony.apps.authentication.jwt.exception.JwtProcessingException;
import org.symphonyoss.symphony.apps.authentication.jwt.JwtService;
import org.symphonyoss.symphony.apps.authentication.jwt.model.JwtPayload;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link AuthenticationFilter}
 *
 * Created by robson on 09/01/18.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationFilterTest {

  private static final String AUTHORIZATION_HEADER = "Authorization";

  private static final String INVALID_AUTHORIZATION_HEADER = "invalid";

  private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";

  private static final String MOCK_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9."
      + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9."
      + "EkN-DOsnsuRjRO6BxXemmJDm3HbxrbRzXglbN2S4sOkopdU4IsDxTI8jO19W_A4K8ZPJij"
      + "NLis4EZsHeY559a4DFOd50_OqgHGuERTqYZyuhtF39yxJPAjUESwxk2J5k_4zM3O-vtd1G"
      + "hyo4IbqKKSy6J9mTniYJPenn5-HIirE";

  private static final String MOCK_SERVLET_PATH = "/servlet/path";

  private static final String GET_METHOD = "GET";

  private static final String OPTIONS_METHOD = "OPTIONS";

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain chain;

  @Mock
  private PrintWriter writer;

  @Mock
  private JwtService service;

  @Mock
  private FilterConfig config;

  @Spy
  private List<String> excludedPaths = new ArrayList<>();

  @InjectMocks
  private AuthenticationFilter filter;

  @Before
  public void init() throws IOException {
    String authorizationHeader = AUTHORIZATION_HEADER_PREFIX + MOCK_JWT;

    doReturn(authorizationHeader).when(request).getHeader(AUTHORIZATION_HEADER);
    doReturn(writer).when(response).getWriter();

    doReturn(GET_METHOD).when(request).getMethod();
  }

  @Test
  public void testMissingAuthorizationHeader() throws IOException, ServletException {
    doReturn(null).when(request).getHeader(AUTHORIZATION_HEADER);

    filter.doFilter(request, response, chain);

    verify(response, times(1)).setContentType(APPLICATION_JSON);
    verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(writer, times(1)).write("{\"info\":\"Missing JWT\"}");
  }

  @Test
  public void testInvalidAuthorizationHeader() throws IOException, ServletException {
    doReturn(INVALID_AUTHORIZATION_HEADER).when(request).getHeader(AUTHORIZATION_HEADER);

    filter.doFilter(request, response, chain);

    verify(response, times(1)).setContentType(APPLICATION_JSON);
    verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(writer, times(1)).write("{\"info\":\"Missing JWT\"}");
  }

  @Test
  public void testInvalidJwt() throws IOException, ServletException, JwtProcessingException {
    doThrow(JwtProcessingException.class).when(service).parseJwtPayload(MOCK_JWT);

    filter.doFilter(request, response, chain);

    verify(response, times(1)).setContentType(APPLICATION_JSON);
    verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(writer, times(1)).write("{\"info\":\"Invalid JWT\"}");
  }

  @Test
  public void testUnexpectedError() throws IOException, ServletException, JwtProcessingException {
    doThrow(RuntimeException.class).when(service).parseJwtPayload(MOCK_JWT);

    filter.doFilter(request, response, chain);

    verify(response, times(1)).setContentType(APPLICATION_JSON);
    verify(response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(writer, times(1)).write("{\"info\":\"Unexpected error, please contact the system administrator\"}");
  }

  @Test
  public void testSuccess() throws IOException, ServletException, JwtProcessingException {
    JwtPayload payload = new JwtPayload();
    doReturn(payload).when(service).parseJwtPayload(MOCK_JWT);

    filter.doFilter(request, response, chain);

    verify(request, times(1)).setAttribute(USER_INFO_ATTRIBUTE, payload);
    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  public void testExcludedPaths() throws IOException, ServletException {
    doReturn(MOCK_SERVLET_PATH).when(request).getServletPath();

    this.excludedPaths.add(MOCK_SERVLET_PATH);

    filter.doFilter(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
    verify(request, never()).getHeader(AUTHORIZATION_HEADER);
  }

  @Test
  public void testHttpMethodNotAllowed() throws IOException, ServletException {
    doReturn(OPTIONS_METHOD).when(request).getMethod();
    doReturn(MOCK_SERVLET_PATH).when(request).getServletPath();

    this.excludedPaths.add(MOCK_SERVLET_PATH);

    filter.doFilter(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
    verify(request, never()).getHeader(AUTHORIZATION_HEADER);
  }
}
