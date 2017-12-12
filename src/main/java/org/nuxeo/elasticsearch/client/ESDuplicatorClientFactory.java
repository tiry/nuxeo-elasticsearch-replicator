/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.client;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchEmbeddedServerConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;

/**
 * @since 9.3
 */
public class ESDuplicatorClientFactory extends ESRestClientFactory  {
	
    private static final Log log = LogFactory.getLog(ESDuplicatorClientFactory.class);

    @Override
    public ESClient create(ElasticSearchEmbeddedNode node, ElasticSearchClientConfig config) {
        if (node != null) {
            return createLocalRestClient(node.getConfig());
        }
        return createRestClient(config);
    }

    protected ESClient createLocalRestClient(ElasticSearchEmbeddedServerConfig serverConfig) {
        if (!serverConfig.httpEnabled()) {
            throw new IllegalArgumentException(
                    "Embedded configuration has no HTTP port enable, use TransportClient instead of Rest");
        }
        RestClient lowLevelRestClient = RestClient.builder(
                new HttpHost("localhost", Integer.parseInt(serverConfig.getHttpPort()))).build();
        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClient);
        // checkConnection(client);
        return new ESDuplicatorClient(lowLevelRestClient, client);
    }

    protected ESClient createRestClient(ElasticSearchClientConfig config) {
        String addressList = config.getOption("addressList", "");
        if (addressList.isEmpty()) {
            throw new IllegalArgumentException("No addressList option provided cannot connect RestClient");
        }
        String[] hosts = addressList.split(",");
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        int i = 0;
        for (String host : hosts) {
            httpHosts[i++] = HttpHost.create(host);
        }
        
        RestClientBuilder builder = RestClient.builder(httpHosts)
                                              .setRequestConfigCallback(
                                                      requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(
                                                              getConnectTimeoutMs(config)).setSocketTimeout(
                                                                      getSocketTimeoutMs(config)))
                                              .setMaxRetryTimeoutMillis(getConnectTimeoutMs(config));
        if (StringUtils.isNotBlank(config.getOption(AUTH_USER_OPT))
                || StringUtils.isNotBlank(config.getOption(KEYSTORE_PATH_OPT))) {
            addClientCallback(config, builder);
        }
        RestClient lowLevelRestClient = builder.build();
        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClient);
        // checkConnection(client);
        return new ESDuplicatorClient(lowLevelRestClient, client);
    }

    private void addClientCallback(ElasticSearchClientConfig config, RestClientBuilder builder) {
        BasicCredentialsProvider credentialProvider = getCredentialProvider(config);
        SSLContext sslContext = getSslContext(config);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (sslContext != null) {
                httpClientBuilder.setSSLContext(sslContext);
            }
            if (credentialProvider != null) {
                httpClientBuilder.setDefaultCredentialsProvider(credentialProvider);
            }
            return httpClientBuilder;
        });
    }

}
