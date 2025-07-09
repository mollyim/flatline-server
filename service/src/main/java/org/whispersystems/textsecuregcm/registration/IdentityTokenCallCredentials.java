/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.whispersystems.textsecuregcm.registration;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ExternalAccountCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityTokenCallCredentials extends CallCredentials implements Managed {
  private static final Duration IDENTITY_TOKEN_LIFETIME = Duration.ofHours(1);
  private static final Duration IDENTITY_TOKEN_REFRESH_BUFFER = Duration.ofMinutes(10);

  static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
      Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

  private static final Logger logger = LoggerFactory.getLogger(IdentityTokenCallCredentials.class);

  private final Retry retry;
  private final GoogleCredentials impersonatedCredentials;
  private final String audience;
  private final ScheduledFuture<?> scheduledFuture;

  private volatile Pair<String, RuntimeException> currentIdentityToken;

  IdentityTokenCallCredentials(
      final RetryConfig retryConfig,
      final GoogleCredentials impersonatedCredentials,
      final String audience,
      final ScheduledExecutorService scheduledExecutorService) {
    this.impersonatedCredentials = impersonatedCredentials;
    this.audience = audience;
    this.retry = Retry.of("identity-token-fetch", retryConfig);
    scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this::refreshIdentityToken,
        IDENTITY_TOKEN_LIFETIME.minus(IDENTITY_TOKEN_REFRESH_BUFFER).toMillis(),
        IDENTITY_TOKEN_LIFETIME.minus(IDENTITY_TOKEN_REFRESH_BUFFER).toMillis(),
        TimeUnit.MILLISECONDS);
  }

  public static IdentityTokenCallCredentials fromCredentialConfig(
      final String credentialConfigJson,
      final String audience,
      final ScheduledExecutorService scheduledExecutorService) throws IOException {
    try (final InputStream configInputStream = new ByteArrayInputStream(
        credentialConfigJson.getBytes(StandardCharsets.UTF_8))) {
      // FLT(uoemai): Use fake access token for self-hosted development.
      // final ExternalAccountCredentials credentials = ExternalAccountCredentials.fromStream(configInputStream);
      // final ImpersonatedCredentials impersonatedCredentials = ImpersonatedCredentials.create(credentials,
      //    credentials.getServiceAccountEmail(), null, List.of(), (int) IDENTITY_TOKEN_LIFETIME.toSeconds());
      AccessToken fakeToken = new AccessToken("fake-token",
          new Date(System.currentTimeMillis() + 3600_000));
      GoogleCredentials impersonatedCredentials = GoogleCredentials.newBuilder()
          .setAccessToken(fakeToken)
          .build();

      final IdentityTokenCallCredentials identityTokenCallCredentials = new IdentityTokenCallCredentials(
          RetryConfig.custom()
              .retryOnException(throwable -> true)
              .maxAttempts(Integer.MAX_VALUE)
              .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                      Duration.ofMillis(100), 1.5, Duration.ofSeconds(5)))
              .build(), impersonatedCredentials, audience, scheduledExecutorService);

      // Make sure credentials are initially populated
      identityTokenCallCredentials.refreshIdentityToken();

      return identityTokenCallCredentials;
    }
  }

  @VisibleForTesting
  void refreshIdentityToken() {
    retry.executeRunnable(() -> {
      // FLT(uoemai): Use fake identity token for self-hosted development.
      // impersonatedCredentials.getSourceCredentials().refresh();
      // this.currentIdentityToken = Pair.of(
      //    impersonatedCredentials.idTokenWithAudience(audience, null).getTokenValue(),
      //    null);
      this.currentIdentityToken = Pair.of(
          "fake-identity-token",
          null);
    });
  }

  @Override
  public void applyRequestMetadata(final RequestInfo requestInfo,
      final Executor appExecutor,
      final MetadataApplier applier) {

    final Pair<String, RuntimeException> pair = currentIdentityToken;
    if (pair.getRight() != null) {
      throw pair.getRight();
    }

    final String identityTokenValue = pair.getLeft();

    if (identityTokenValue != null) {
      final Metadata metadata = new Metadata();
      metadata.put(AUTHORIZATION_METADATA_KEY, "Bearer " + identityTokenValue);

      applier.apply(metadata);
    }
  }

  @Override
  public void stop() {
    synchronized (this) {
      if (!scheduledFuture.isDone()) {
        scheduledFuture.cancel(true);
      }
    }
  }
}
