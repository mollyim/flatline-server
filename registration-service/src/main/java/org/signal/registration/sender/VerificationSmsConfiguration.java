/*
 * Copyright 2022 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.sender;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ConfigurationProperties("verification.sms")
public class VerificationSmsConfiguration {
  @NotBlank
  private String androidAppHash;

  private List<@NotBlank String> supportedLanguages = List.of(
      "af",
      "ar",
      "az",
      "bg",
      "bn",
      "bs",
      "ca",
      "cs",
      "da",
      "de",
      "el",
      "en",
      "es",
      "et",
      "eu",
      "fa",
      "fi",
      "fr",
      "ga",
      "gl",
      "gu",
      "he",
      "hi",
      "hr",
      "hu",
      "id",
      "it",
      "ja",
      "ka",
      "kk",
      "km",
      "kn",
      "ko",
      "ky",
      "lt",
      "lv",
      "mk",
      "ml",
      "mr",
      "ms",
      "my",
      "nb",
      "nl",
      "pa",
      "pl",
      "pt",
      "pt-BR",
      "pt-PT",
      "ro",
      "ru",
      "sk",
      "sl",
      "sq",
      "sr",
      "sv",
      "sw",
      "ta",
      "te",
      "th",
      "tl",
      "tr",
      "ug",
      "uk",
      "ur",
      "vi",
      "zh",
      "zh-CN",
      "zh-HK",
      "zh-TW",
      "zh-YU");

  /**
   * A map of region codes (e.g. "US" or "MX") to message body "variant" names. Variant names should have corresponding
   * entries in the SMS string table (e.g. if this map names a "short" variant, the string table should include entries
   * for `verification.sms.ios.short`, `verification.sms.android.short`, and `verification.sms.generic.short`).
   */
  private Map<String, @NotBlank String> messageVariantsByRegion = Collections.emptyMap();

  public String getAndroidAppHash() {
    return androidAppHash;
  }

  public void setAndroidAppHash(final String androidAppHash) {
    this.androidAppHash = androidAppHash;
  }

  public List<String> getSupportedLanguages() {
    return supportedLanguages;
  }

  public void setSupportedLanguages(final List<String> supportedLanguages) {
    this.supportedLanguages = supportedLanguages;
  }

  public Map<String, String> getMessageVariantsByRegion() {
    return messageVariantsByRegion;
  }

  public void setMessageVariantsByRegion(final Map<String, String> messageVariantsByRegion) {
    // Coerce region codes to uppercase
    this.messageVariantsByRegion = messageVariantsByRegion.entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(entry -> entry.getKey().toUpperCase(), Map.Entry::getValue));
  }
}
