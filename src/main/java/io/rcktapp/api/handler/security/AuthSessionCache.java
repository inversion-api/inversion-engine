/**
 * 
 */
package io.rcktapp.api.handler.security;

import io.rcktapp.api.User;

/**
 * @author tc-rocket
 *
 */
public interface AuthSessionCache
{
   public User get(String sessionKey);
   public void put(String sessionKey, User user);
   public void remove(String sessionKey);
}
