/**
 * 
 */
package io.rocketpartners.cloud.api.handler.security;

import io.rocketpartners.cloud.api.User;

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
