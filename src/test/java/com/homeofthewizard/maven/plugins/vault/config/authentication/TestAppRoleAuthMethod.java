package com.homeofthewizard.maven.plugins.vault.config.authentication;

import com.homeofthewizard.maven.plugins.vault.config.Server;
import com.homeofthewizard.maven.plugins.vault.config.authentication.approle.AppRoleAuthMethod;
import com.homeofthewizard.maven.plugins.vault.config.authentication.approle.AppRoleCredentials;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Auth;
import io.github.jopenlibs.vault.response.AuthResponse;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.TreeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestAppRoleAuthMethod {
    @Test
    public void testLogin() throws VaultException {
        var roleIdTag = AppRoleCredentials.class.getDeclaredFields()[0].getName();
        var roleSecretTag = AppRoleCredentials.class.getDeclaredFields()[1].getName();
        TreeMap map = new TreeMap<>();
        map.put(roleIdTag,"roleId123");
        map.put(roleSecretTag, "roleSecretPWD");
        Map<String, TreeMap> appRoleCredentials = Map.of(
                AuthenticationMethodFactory.APP_ROLE_TAG, map
        );
        var server = new Server("URL",null,true,null,appRoleCredentials,null,null,true,null);
        var authMock = Mockito.mock(Auth.class);
        var authResponseMock = Mockito.mock(AuthResponse.class);
        when(authResponseMock.getAuthClientToken()).thenReturn("TOKEN");
        when(authMock.loginByAppRole(any(),any())).thenReturn(authResponseMock);
        var appRoleAuthMethod = new AppRoleAuthMethod(authMock, server);

        appRoleAuthMethod.login();

        Assert.assertTrue(server.getToken().equals("TOKEN"));
    }
}
