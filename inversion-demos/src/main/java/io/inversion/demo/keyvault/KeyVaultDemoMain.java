/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.demo.keyvault;

import com.azure.core.http.rest.PagedIterable;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import io.inversion.Api;
import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcDb;
import io.inversion.spring.main.InversionMain;
import io.inversion.config.Config;
import io.inversion.utils.Utils;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Example of how database connectivity settings can be pulled from an Azure KeyVault at runtime.
 *
 * @see <a href="https://inversion-api.github.io/inversion-engine/javadoc/io/inversion/jdbc/JdbcDb.html">io.inversion.jdbc.JdbcDb</a>
 * @see <a href="https://inversion-api.github.io/inversion-engine/javadoc/io/inversion/utils/Config.html">io.inversion.config.Config</a>
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration2/CombinedConfiguration.html">org.apache.commons.configuration2.CombinedConfiguration</a>
 */
public class KeyVaultDemoMain {
//    public static void main(String[] args) {
//        //-- pull all the secrets from the KeyVault
//        //-- for demo purposes, assume secrets for 'myDb.driver','myDb.url',
//        //-- 'myDb.user','myDb.pass' are in the vault
//        PropertiesConfiguration secretsConf = new PropertiesConfiguration();
//
//        //-- TODO: this KeyVault integration has not been tested.  The main point of this example is
//        //-- to show you how to customize the configuration, not how to use KeyVault.
//        String keyVaultName = System.getenv("KEY_VAULT_NAME");
//        String kvUri        = "https://" + keyVaultName + ".vault.azure.net";
//
//        SecretClient secretClient = new SecretClientBuilder().//
//                vaultUrl(kvUri).//
//                credential(new DefaultAzureCredentialBuilder().build()).buildClient();
//
//        PagedIterable<SecretProperties> secretIt = secretClient.listPropertiesOfSecrets();
//
//        //LinkedHashSet<String> names = new LinkedHashSet<>();
//        //secretIt.streamByPage().forEach(resp -> resp.getElements().stream().map(props -> names.add(props.getName())));
//        Set<String> names = secretIt.streamByPage().flatMap(page -> page.getElements().stream()).map(SecretProperties::getName).collect(Collectors.toSet());
//
//        for (String name : names) {
//            KeyVaultSecret sec = secretClient.getSecret(name);
//            secretsConf.setProperty(sec.getName(), sec.getValue());
//        }
//        //-- end secrets lookup
//
//        //-- now we are going to cause the default configuration to be
//        //-- loaded and then augment it with the key vault properties
//        String configPath    = Utils.findSysEnvProp("configPath");
//        String configProfile = Utils.findSysEnvProp("configProfile", "profile");
//
//        Config.loadConfiguration(KeyVaultDemoMain.class, configPath, configProfile);//this loads the default configuration
//        CompositeConfiguration config = Config.getConfiguration();
//
//        //-- add the secrets to the start of the composite list so that key vault values are pulled first
//        config.addConfigurationFirst(secretsConf);
//
//        //-- now wire up your api.
//        //-- bean properties for all named objects will be reflectively set
//
//        Api api = new Api()//
//                .withDb(new JdbcDb().withName("myDb"))//
//                .withEndpoint(new DbAction());
//
//        //-- runs the Api as a spring boot app
//        InversionMain.run(api);
//    }

}
