package com.homeofthewizard.maven.plugins.vault.config.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Auth;

import java.util.Map;

/**
 * An abstract class that gives a skeleton for classes that implements authentication method to Hashicorp Vault.
 * The generic type argument is the POJO that gives the specific credentials/tokens to each authentication method.
 * @param <T> Generic type arguments that defines the POJO class of the authentication credentials
 */
public abstract class AuthenticationMethod<T> {

  protected Auth auth;

  protected Class<T> credentialObjectClass;

  /**
   * Initializes a new instance of the {@link AuthenticationMethod} class.
   * @param auth Auth
   */
  public AuthenticationMethod(Auth auth, Class<T> credentialObjectClass) {
    this.auth = auth;
    this.credentialObjectClass = credentialObjectClass;
  }

  public abstract void login() throws VaultException;

  /**
   * Deserialize the Map<\String,Object\> from the server config that contains the authentication's credentials,
   * gives back an object of the generic type given by the implementation of AuthenticationMethod.class
   * @param authMethodMap Map<\String,Object\>
   * @return T is the generic type argument of the class
   */
  public T getAuthCredentials(Map<String,String> authMethodMap) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(authMethodMap, credentialObjectClass);
  }
}
