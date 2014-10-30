/*
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.bonitasoft.console.common.server.login.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bonitasoft.console.common.server.login.LoginManager;
import org.bonitasoft.console.common.server.preferences.properties.DynamicPermissionsChecks;
import org.bonitasoft.console.common.server.preferences.properties.ResourcesPermissionsMapping;
import org.bonitasoft.engine.api.permission.APICallContext;
import org.bonitasoft.engine.exception.ExecutionException;
import org.bonitasoft.engine.exception.NotFoundException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RestAPIAuthorizationFilterTest {

    @Mock
    private ResourcesPermissionsMapping resourcesPermissionsMapping;
    @Mock
    private DynamicPermissionsChecks dynamicPermissionsChecks;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private APISession apiSession;
    @Mock
    private HttpSession httpSession;

    private final RestAPIAuthorizationFilter restAPIAuthorizationFilter = new RestAPIAuthorizationFilter();

    private final String username = "john";

    @Before
    public void before() {
        doReturn(httpSession).when(request).getSession();
        doReturn("").when(request).getQueryString();
        doReturn(apiSession).when(httpSession).getAttribute(LoginManager.API_SESSION_PARAM_KEY);
        doReturn(1l).when(apiSession).getTenantId();
        doReturn(false).when(apiSession).isTechnicalUser();
        doReturn("john").when(apiSession).getUserName();
    }

    private Set<String> initSpy(final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy) throws ServletException {
        doReturn("GET").when(request).getMethod();

        final Set<String> permissions = new HashSet<String>(Arrays.asList("plop"));
        doReturn(permissions).when(httpSession).getAttribute(LoginManager.PERMISSIONS_SESSION_PARAM_KEY);
        doReturn(resourcesPermissionsMapping).when(restAPIAuthorizationFilterSpy).getResourcesPermissionsMapping(1);
        doReturn(dynamicPermissionsChecks).when(restAPIAuthorizationFilterSpy).getDynamicPermissionsChecks(1);
        doReturn("").when(restAPIAuthorizationFilterSpy).getRequestBody(request);
        return permissions;
    }

    @Test
    public void should_checkPermissions_call_static_check_if_secu_is_enabled() throws Exception {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final Set<String> permissions = initSpy(restAPIAuthorizationFilterSpy);
        doReturn(true).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);
        doReturn(true).when(restAPIAuthorizationFilterSpy).staticCheck(anyString(), anyString(), anyString(), anyString(), anySetOf(String.class),
                any(ResourcesPermissionsMapping.class), eq(username));
        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isTrue();
        verify(restAPIAuthorizationFilterSpy).staticCheck("GET", "bpm", "case", null, permissions, resourcesPermissionsMapping, username);
    }

    @Test
    public void should_checkPermissions_call_dynamic_check_if_secu_is_enabled_and_static_check_does_not_authorize() throws Exception {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        initSpy(restAPIAuthorizationFilterSpy);
        doReturn(true).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);
        doReturn(false).when(restAPIAuthorizationFilterSpy).staticCheck(anyString(), anyString(), anyString(), anyString(), anySetOf(String.class),
                any(ResourcesPermissionsMapping.class), eq(username));
        doReturn(true).when(restAPIAuthorizationFilterSpy).dynamicCheck(anyString(), anyString(), anyString(), anyString(), any(APISession.class),
                any(DynamicPermissionsChecks.class), anyString(), anyString());
        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isTrue();
        verify(restAPIAuthorizationFilterSpy).dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");
    }

    @Test
    public void should_checkPermissions_do_not_call_check_if_technical() throws Exception {
        doReturn(true).when(apiSession).isTechnicalUser();
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final Set<String> permissions = initSpy(restAPIAuthorizationFilterSpy);
        doReturn(true).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);
        doReturn(true).when(restAPIAuthorizationFilterSpy).staticCheck(anyString(), anyString(), anyString(), anyString(), anySetOf(String.class),
                any(ResourcesPermissionsMapping.class), eq(username));
        doReturn(true).when(restAPIAuthorizationFilterSpy).dynamicCheck(anyString(), anyString(), anyString(), anyString(), any(APISession.class),
                any(DynamicPermissionsChecks.class), anyString(), anyString());
        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isTrue();
        verify(restAPIAuthorizationFilterSpy, times(0)).staticCheck("GET", "bpm", "case", null, permissions, resourcesPermissionsMapping, username);
        verify(restAPIAuthorizationFilterSpy, times(0)).dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");
    }

    @Test
    public void should_checkPermissions_do_nothing_if_secu_is_disabled() throws Exception {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final Set<String> permissions = initSpy(restAPIAuthorizationFilterSpy);
        doReturn(false).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);

        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isTrue();
        verify(restAPIAuthorizationFilterSpy, times(0)).staticCheck("GET", "bpm", "case", null, permissions, resourcesPermissionsMapping, username);
        verify(restAPIAuthorizationFilterSpy, times(0)).dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");
    }

    @Test
    public void should_checkPermissions_return_true_if_static_authorized() throws Exception {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final Set<String> permissions = initSpy(restAPIAuthorizationFilterSpy);
        doReturn(true).when(restAPIAuthorizationFilterSpy).staticCheck("GET", "bpm", "case", null, permissions, resourcesPermissionsMapping, username);
        doReturn(true).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);

        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void should_checkPermissions_return_false_if_satic_and_dynamic_not_authorized() throws Exception {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final Set<String> permissions = initSpy(restAPIAuthorizationFilterSpy);
        doReturn(false).when(restAPIAuthorizationFilterSpy).staticCheck("GET", "bpm", "case", null, permissions, resourcesPermissionsMapping, username);
        doReturn(false).when(restAPIAuthorizationFilterSpy).dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");
        doReturn(true).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);

        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void should_checkPermissions_return_true_if_satic_not_authorized_and_dynamic_authorized() throws Exception {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final Set<String> permissions = initSpy(restAPIAuthorizationFilterSpy);
        doReturn(false).when(restAPIAuthorizationFilterSpy).staticCheck("GET", "bpm", "case", null, permissions, resourcesPermissionsMapping, username);
        doReturn(true).when(restAPIAuthorizationFilterSpy).dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");
        doReturn(true).when(restAPIAuthorizationFilterSpy).isApiAuthorizationsCheckEnabled(1l);

        //when
        final boolean isAuthorized = restAPIAuthorizationFilterSpy.checkPermissions(request, "bpm", "case", null);

        //then
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void should_checkPermissions_parse_the_request() throws Exception {
        doReturn("API/bpm/case/15").when(request).getPathInfo();
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        doReturn(true).when(restAPIAuthorizationFilterSpy).checkPermissions(eq(request), eq("bpm"), eq("case"), eq(APIID.makeAPIID(15l)));

        //when
        restAPIAuthorizationFilterSpy.checkPermissions(request);

        //then
        verify(restAPIAuthorizationFilterSpy).checkPermissions(eq(request), eq("bpm"), eq("case"), eq(APIID.makeAPIID(15l)));
    }

    @Test
    public void test_staticCheck_authorized() throws Exception {
        final Set<String> userPermissions = new HashSet<String>(Arrays.asList("MyPermission", "AnOtherPermission"));
        returnPermissionFor("GET", "bpm", "case", null, Arrays.asList("CasePermission", "AnOtherPermission"));

        final boolean isAuthorized = restAPIAuthorizationFilter.staticCheck("GET", "bpm", "case", null, userPermissions, resourcesPermissionsMapping, username);

        assertThat(isAuthorized).isTrue();

    }

    @Test
    public void test_staticCheck_unauthorized() throws Exception {
        final Set<String> userPermissions = new HashSet<String>(Arrays.asList("MyPermission", "AnOtherPermission"));
        returnPermissionFor("GET", "bpm", "case", null, Arrays.asList("CasePermission", "SecondPermission"));

        final boolean isAuthorized = restAPIAuthorizationFilter.staticCheck("GET", "bpm", "case", null, userPermissions, resourcesPermissionsMapping, username);

        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void test_dynamicCheck_authorized() throws Exception {
        doReturn("className").when(dynamicPermissionsChecks).getResourceScript("GET", "bpm", "case");
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final APICallContext apiCallContext = new APICallContext("GET", "bpm", "case", null, "", "");
        doReturn(true).when(restAPIAuthorizationFilterSpy).checkWithScript("GET", "bpm", "case", null, apiSession, "className", apiCallContext);

        final boolean isAuthorized = restAPIAuthorizationFilterSpy.dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");

        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void test_dynamicCheck_unauthorized() throws Exception {
        doReturn("className").when(dynamicPermissionsChecks).getResourceScript("GET", "bpm", "case");
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final APICallContext apiCallContext = new APICallContext("GET", "bpm", "case", null, "", "");
        doReturn(false).when(restAPIAuthorizationFilterSpy).checkWithScript("GET", "bpm", "case", null, apiSession, "className", apiCallContext);

        final boolean isAuthorized = restAPIAuthorizationFilterSpy.dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");

        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void should_dynamicCheck_return_false_if_the_script_execution_fails() throws Exception {
        doReturn("className").when(dynamicPermissionsChecks).getResourceScript("GET", "bpm", "case");
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final APICallContext apiCallContext = new APICallContext("GET", "bpm", "case", null, "", "");
        doThrow(ExecutionException.class).when(restAPIAuthorizationFilterSpy).checkWithScript("GET", "bpm", "case", null, apiSession, "className",
                apiCallContext);

        final boolean isAuthorized = restAPIAuthorizationFilterSpy.dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");

        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void should_dynamicCheck_return_false_if_the_script_is_not_found() throws Exception {
        doReturn("className").when(dynamicPermissionsChecks).getResourceScript("GET", "bpm", "case");
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        final APICallContext apiCallContext = new APICallContext("GET", "bpm", "case", null, "", "");
        doThrow(NotFoundException.class).when(restAPIAuthorizationFilterSpy).checkWithScript("GET", "bpm", "case", null, apiSession, "className",
                apiCallContext);

        final boolean isAuthorized = restAPIAuthorizationFilterSpy.dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");

        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void should_dynamicCheck_return_false_on_resource_with_no_script() throws Exception {
        final boolean isAuthorized = restAPIAuthorizationFilter.dynamicCheck("GET", "bpm", "case", null, apiSession, dynamicPermissionsChecks, "", "");

        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void test_staticCheck_unauthorized_on_resource_with_id_even_if_permission_in_general_is_there() throws Exception {
        final Set<String> userPermissions = new HashSet<String>(Arrays.asList("MyPermission", "AnOtherPermission"));
        returnPermissionFor("GET", "bpm", "case", null, Arrays.asList("CasePermission", "AnOtherPermission"));
        returnPermissionFor("GET", "bpm", "case", "12", Arrays.asList("CasePermission", "SecondPermission"));

        final boolean isAuthorized = restAPIAuthorizationFilter.staticCheck("GET", "bpm", "case", "12", userPermissions, resourcesPermissionsMapping, username);

        assertThat(isAuthorized).isFalse();

    }

    @Test
    public void test_staticCheck_authorized_on_resource_with_id() throws Exception {
        final Set<String> userPermissions = new HashSet<String>(Arrays.asList("MyPermission", "AnOtherPermission"));
        returnPermissionFor("GET", "bpm", "case", "12", Arrays.asList("CasePermission", "MyPermission"));

        final boolean isAuthorized = restAPIAuthorizationFilter.staticCheck("GET", "bpm", "case", "12", userPermissions, resourcesPermissionsMapping, username);

        assertThat(isAuthorized).isTrue();

    }

    @Test
    public void test_staticCheck_resource_with_id_should_check_parent_if_no_rule() throws Exception {
        final Set<String> userPermissions = new HashSet<String>(Arrays.asList("MyPermission", "AnOtherPermission"));
        returnPermissionFor("GET", "bpm", "case", null, Arrays.asList("CasePermission", "MyPermission"));

        final boolean isAuthorized = restAPIAuthorizationFilter.staticCheck("GET", "bpm", "case", "12", userPermissions, resourcesPermissionsMapping, username);

        assertThat(isAuthorized).isTrue();

    }

    private void returnPermissionFor(final String method, final String apiName, final String resourceName, final String resourceId, final List<String> toBeReturned) {
        if (resourceId != null) {
            doReturn(toBeReturned).when(resourcesPermissionsMapping).getResourcePermissions(method, apiName, resourceName, resourceId);
        } else {
            doReturn(toBeReturned).when(resourcesPermissionsMapping).getResourcePermissions(method, apiName, resourceName);
        }
    }

    @Test
    public void should_checkValidCondition_check_session_is_platform() throws ServletException {
        doReturn("API/platform/plop").when(request).getRequestURI();
        doReturn(mock(PlatformSession.class)).when(httpSession).getAttribute(RestAPIAuthorizationFilter.PLATFORM_SESSION_PARAM_KEY);
        //when
        final boolean isValid = restAPIAuthorizationFilter.checkValidCondition(request, response);

        assertThat(isValid).isTrue();
    }

    @Test
    public void should_checkValidCondition_check_unauthorized_if_no_platform_session() throws ServletException {
        doReturn("API/platform/plop").when(request).getRequestURI();
        doReturn(null).when(httpSession).getAttribute(RestAPIAuthorizationFilter.PLATFORM_SESSION_PARAM_KEY);
        //when
        final boolean isValid = restAPIAuthorizationFilter.checkValidCondition(request, response);

        assertThat(isValid).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void should_checkValidCondition_check_unauthorized_if_no_tenant_session() throws ServletException {
        doReturn(null).when(httpSession).getAttribute(LoginManager.API_SESSION_PARAM_KEY);
        doReturn("API/bpm/case/15").when(request).getRequestURI();
        //when
        final boolean isValid = restAPIAuthorizationFilter.checkValidCondition(request, response);

        assertThat(isValid).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }


    @Test
    public void should_checkValidCondition_check_permission_if_is_tenant_is_forbidden() throws ServletException {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        doReturn("API/bpm/case/15").when(request).getRequestURI();
        doReturn(false).when(restAPIAuthorizationFilterSpy).checkPermissions(request);

        //when
        final boolean isValid = restAPIAuthorizationFilterSpy.checkValidCondition(request, response);

        assertThat(isValid).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }


    @Test
    public void should_checkValidCondition_check_permission_if_is_tenant_is_ok() throws ServletException {
        final RestAPIAuthorizationFilter restAPIAuthorizationFilterSpy = spy(restAPIAuthorizationFilter);
        doReturn("API/bpm/case/15").when(request).getRequestURI();
        doReturn(true).when(restAPIAuthorizationFilterSpy).checkPermissions(request);

        //when
        final boolean isValid = restAPIAuthorizationFilterSpy.checkValidCondition(request, response);

        assertThat(isValid).isTrue();
    }

    @Test(expected = ServletException.class)
    public void should_checkValidCondition_catch_runtime() throws ServletException {
        doThrow(new RuntimeException()).when(request).getRequestURI();

        //when
        restAPIAuthorizationFilter.checkValidCondition(request, response);

    }
}