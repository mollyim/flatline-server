/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.sender.messagebird.classic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.signal.registration.sender.ClientType;
import org.signal.registration.sender.VerificationTTSBodyProvider;
import org.signal.registration.sender.VerificationVoiceConfiguration;

class MessageBirdVoiceTTSBodyProviderTest {

  private MessageBirdVoiceTTSBodyProvider bodyProvider;

  @BeforeEach
  void setUp() {
    final List<String> baseTTSProviderSupportedLanguages = List.of("id", "ro");
    final List<String> messageBirdTTSProviderSupportedLanguages = List.of("pt-pt", "ro-ro");

    final VerificationTTSBodyProvider baseTTSBodyProvider = new VerificationTTSBodyProvider(
        new VerificationVoiceConfiguration(baseTTSProviderSupportedLanguages),
        new SimpleMeterRegistry());

    bodyProvider = new MessageBirdVoiceTTSBodyProvider(
        new MessageBirdVoiceConfiguration(1, Duration.ofMinutes(10), messageBirdTTSProviderSupportedLanguages),
        baseTTSBodyProvider
    );
  }

  
  @Test
  void supportsLanguage() {
    assertFalse(bodyProvider.supportsLanguage(Locale.LanguageRange.parse("id")));
    assertTrue(bodyProvider.supportsLanguage(Locale.LanguageRange.parse("ro")));
    assertTrue(bodyProvider.supportsLanguage(Locale.LanguageRange.parse("ro-RO")));
    assertFalse(bodyProvider.supportsLanguage(Locale.LanguageRange.parse("pt")));
    assertFalse(bodyProvider.supportsLanguage(Locale.LanguageRange.parse("pt-BR")));
  }

  @ParameterizedTest
  @MethodSource
  void getVerificationBody(final String clientLanguageRange, final String expectedBody) {

    final Phonenumber.PhoneNumber phoneNumber = PhoneNumberUtil.getInstance().getExampleNumber("RO");

    final String actualBody = bodyProvider.getVerificationBody(
        phoneNumber,
        ClientType.ANDROID_WITH_FCM,
        "123456",
        Locale.LanguageRange.parse(clientLanguageRange)
    );

    assertEquals(expectedBody, actualBody);
  }

  static List<Arguments> getVerificationBody() {
    return  List.of(
        Arguments.of("id", "Your Signal verification code is, 1, 2, 3, 4, 5, 6"),
        Arguments.of("pt", "Your Signal verification code is, 1, 2, 3, 4, 5, 6"),
        Arguments.of("ro", "Codul tău de verificare Signal este, 1, 2, 3, 4, 5, 6"),
        Arguments.of("ro-RO", "Codul tău de verificare Signal este, 1, 2, 3, 4, 5, 6")
    );
  }

}
