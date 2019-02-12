/**
 * 
 */
package io.rocketpartners.cloud.handler.security;

import io.rocketpartners.cloud.model.User;

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
