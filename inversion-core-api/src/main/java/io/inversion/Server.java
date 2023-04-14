/*
 * Copyright (c) 2015-2021 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion;

import io.inversion.utils.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class Server {// extends Rule<Server> {

    String name = null;

    LinkedHashMap<Url, ServerMatcher> urls = new LinkedHashMap();

    boolean documented = true;

    public Server() {
        //System.out.println("Server()<>");
    }

    public Server(String... urls) {
        withUrls(urls);
    }

    public Server(Url... urls) {
        withUrls(urls);
    }

    public String getName() {
        return name;
    }

    public Server withName(String name) {
        this.name = name;
        return this;
    }

    public String toString(){
        return urls.keySet().toString();
    }

    public static class ServerMatcher {
        Url url;
        String protocol;
        Path   host;
        int port = 0;
        Path   path;

        public ServerMatcher(Url url){
            this.url = url;
            protocol = url.getProtocol();
            host = url.getHost() != null ? new Path(url.getHost().replace(".", "/")) : null;
            port = url.getPort();
            path = url.getPath();
            if(path == null)
                path = new Path("*");
            if(!path.endsWithWildcard())
                path.add("*");
        }

        public String toString(){
            if(host != null && host.size() > 0){
                return url.toString();
            }
            if(port > 0){
                return "/" + path + ":" + port;
            }
            else{
                return "/" + path;
            }

        }

        public boolean match(Url url){
//            System.err.println("ServerMatch.match(" + url + ")");
//            System.err.println(" - protocol = " + protocol);
//            System.err.println(" - host     = " + host);
//            System.err.println(" - port     = " + port);
//            System.err.println(" - path     = " + path);
            if(protocol != null && !protocol.equals("//") && !protocol.equals(url.getProtocol())){
//                System.err.println(" <- FALSE protocol does not match");
                return false;
            }

            if(host != null && url.getHostAsPath() != null && !host.matches(url.getHostAsPath())){
//                System.err.println(" <- FALSE host does not match");
                return false;
            }

            if(port > 0 && url.getPort() > 0 && port != url.getPort()){
//                System.err.println(" <- FALSE port does not match");
                return false;
            }

            if(path != null && !path.matches(url.getPath())){
//                System.err.println(" <- FALSE path does not match");
                return false;
            }
//            System.err.println(" <- TRUE");

            return true;
        }

        public String getProtocol() {
            return protocol;
        }

        public Path getHost() {
            if(host == null)
                return new Path();
            return new Path(host);
        }

        public Path getPath() {
            return new Path(path);
        }

        public int getPort() {
            return port;
        }
    }

    public ServerMatcher match(Url url){
        if(urls.size() == 0){
            withUrls("/");
        }
        for(ServerMatcher matcher : urls.values()){
            if(matcher.match(url)){
                return matcher;
            }
        }
        return null;
    }

    public Server withUrls(String... urls) {
        for(String url : urls){
            withUrls(new Url(url));
        }
        return this;
    }

    public Server withUrls(Url... urls) {
        for(Url url : urls){
            this.urls.put(url, new ServerMatcher(url));
        }
        return this;
    }

    public List<Url> getUrls(){
        return new ArrayList(urls.keySet());
    }

    public boolean isDocumented() {
        return documented;
    }

    public Server withDocumented(boolean documented) {
        this.documented = documented;
        return this;
    }

    public List<ServerMatcher> getServerMatches(){
        return new ArrayList<>(this.urls.values());
    }

}
