/*
 * Copyright The Hypertrace Authors
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
 */

package org.hypertrace.agent.filter.opa.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.*;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hypertrace.agent.filter.opa.custom.data.BlockingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpaCommunicator {
  private static final Logger log = LoggerFactory.getLogger(OpaCommunicator.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String PATH = "/v1/data";

  private OkHttpClient httpClient;
  private Request request;

  private BlockingData blockingData;

  protected OpaCommunicator() {}

  public void init(String endpoint, String authToken, boolean skipVerify) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    if (authToken != null && !authToken.isEmpty()) {
      log.info("Adding authentication key");
      builder = withAuth(builder, authToken);
    }
    if (skipVerify) {
      builder = withSkipVerify(builder);
    }
    this.httpClient = builder.build();
    if (endpoint.endsWith("/")) {
      endpoint = endpoint.substring(0, endpoint.length() - 1);
    }
    this.request = new Request.Builder().url(endpoint + PATH).get().build();
  }

  public void pollBlockingData() {
    if (httpClient == null) {
      return;
    }

    Response response;
    try {
      response = httpClient.newCall(request).execute();
    } catch (IOException e) {
      log.warn("Unable to make a successful get call to the OPA service.", e);
      return;
    }

    log.trace("Received response from OPA service: {}", response);
    if (response.isSuccessful()) {
      try {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body().byteStream());
        if (log.isDebugEnabled()) {
          log.debug("Received blocking data from OPA service: {}", jsonNode);
        }
        blockingData =
            OBJECT_MAPPER.treeToValue(
                jsonNode.at("/result/traceable/http/request"), BlockingData.class);
      } catch (IOException e) {
        log.warn("Unable to retrieve blocking data from the OPA service.", e);
      }
    }
  }

  public BlockingData getBlockingData() {
    return blockingData;
  }

  public void clear() {
    httpClient = null;
    request = null;
    blockingData = null;
  }

  private static OkHttpClient.Builder withAuth(OkHttpClient.Builder builder, String authToken) {
    builder.addInterceptor(getAuthInterceptor("Bearer " + authToken));
    return builder;
  }

  private static OkHttpClient.Builder withSkipVerify(OkHttpClient.Builder builder) {
    // Install the all-trusting trust manager
    try {
      TrustManager[] trustAllCertsManagers = getTrustAllCertsManagers();
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCertsManagers, new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCertsManagers[0]);
      builder.hostnameVerifier(getSkipVerifyHostnameVerifier());
    } catch (NoSuchAlgorithmException e) {
      log.warn("Error in getting SSL context. SkipVerify could not be set to true.", e);
    } catch (KeyManagementException e) {
      log.warn("Error in initializing SSL context. SkipVerify could not be set to true.", e);
    }
    return builder;
  }

  private static Interceptor getAuthInterceptor(final String headerValue) {
    return new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        return chain.proceed(
            chain.request().newBuilder().addHeader("Authorization", headerValue).build());
      }
    };
  }

  private static TrustManager[] getTrustAllCertsManagers() {
    return new TrustManager[] {
      new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
            throws CertificateException {}

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
            throws CertificateException {}

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return new java.security.cert.X509Certificate[] {};
        }
      }
    };
  }

  private static HostnameVerifier getSkipVerifyHostnameVerifier() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
  }
}
