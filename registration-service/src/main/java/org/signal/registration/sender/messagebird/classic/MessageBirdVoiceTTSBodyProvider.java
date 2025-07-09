/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.signal.registration.sender.messagebird.classic;

import com.google.common.annotations.VisibleForTesting;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.signal.registration.sender.ClientType;
import org.signal.registration.sender.VerificationTTSBodyProvider;

@Singleton
public class MessageBirdVoiceTTSBodyProvider {

  private final Map<String, String> supportedLanguages;
  private final VerificationTTSBodyProvider verificationTTSBodyProvider;

  public MessageBirdVoiceTTSBodyProvider(
      final MessageBirdVoiceConfiguration messageBirdVoiceConfiguration,
      final VerificationTTSBodyProvider verificationTTSBodyProvider
  ) {
    this.supportedLanguages = supportedLanguages(messageBirdVoiceConfiguration.supportedLanguages());
    this.verificationTTSBodyProvider = verificationTTSBodyProvider;
  }

  public boolean supportsLanguage(List<Locale.LanguageRange> languageRanges) {
    return lookupMessageBirdLanguage(languageRanges).isPresent()
        && verificationTTSBodyProvider.supportsLanguage(languageRanges);
  }

  Optional<String> lookupMessageBirdLanguage(final List<Locale.LanguageRange> languageRanges) {
    return Optional
        .ofNullable(Locale.lookupTag(languageRanges, supportedLanguages.keySet()))
        .map(supportedLanguages::get);
  }

  @VisibleForTesting
  static Map<String, String> supportedLanguages(final List<String> original) {
    // messagebird only supports language tags that include an extension. If there's
    // a tag without an extension, we'd like it to map to one of the tags messagebird supports
    Map<String, String> ret = new HashMap<>();
    for (String lang : original) {
      ret.put(lang, lang);
      ret.putIfAbsent(lang.split("-")[0], lang);
    }
    return ret;
  }

  public String getVerificationBody(Phonenumber.PhoneNumber phoneNumber, ClientType clientType, String verificationCode,
      List<Locale.LanguageRange> languageRanges) {

    final List<Locale.LanguageRange> filteredLanguageRanges = Locale.filterTags(languageRanges, supportedLanguages.keySet())
        .stream().map(Locale.LanguageRange::new)
        .toList();

    return verificationTTSBodyProvider.getVerificationBody(phoneNumber, clientType, verificationCode, filteredLanguageRanges);
  }
}
