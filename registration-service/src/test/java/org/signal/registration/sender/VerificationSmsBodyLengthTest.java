/*
 * Copyright 2022 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.sender;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.MessageSource;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import threegpp.charset.gsm.GSM7BitPackedCharset;
import threegpp.charset.ucs2.UCS2Charset80;

class VerificationSmsBodyLengthTest {

  private static final int SMS_SEGMENT_LENGTH_OCTETS = 140;

  private static final int VERIFICATION_CODE_DIGITS = 6;
  private static final int APP_HASH_LENGTH = 11;

  private static final CharsetEncoder GSM7_ENCODER = new GSM7BitPackedCharset().newEncoder();
  private static final CharsetEncoder UCS2_ENCODER = new UCS2Charset80().newEncoder();

  private static final MessageSource MESSAGE_SOURCE =
      new ResourceBundleMessageSource("org.signal.registration.sender.sms");

  @ParameterizedTest
  @MethodSource
  void messageFitsInSingleSMSSegment(final Locale locale, final String key) throws IOException {
    final MessageSource.MessageContext messageContext =
        MessageSource.MessageContext.of(locale, Map.of(
            "code", RandomStringUtils.insecure().nextNumeric(VERIFICATION_CODE_DIGITS),
            "appHash", RandomStringUtils.insecure().nextAlphanumeric(APP_HASH_LENGTH)));

    final String message = MESSAGE_SOURCE.getRequiredMessage(key, messageContext);

    assertTrue(getEncodedMessageLengthOctets(message) <= SMS_SEGMENT_LENGTH_OCTETS);
  }

  private static int getEncodedMessageLengthOctets(final String message) throws CharacterCodingException {
    if (GSM7_ENCODER.canEncode(message)) {
      return GSM7_ENCODER.encode(CharBuffer.wrap(message)).remaining();
    } else if (UCS2_ENCODER.canEncode(message)) {
      // We subtract 1 here because `threegpp.charset.ucs2.UCS2Charset80` prepends a header byte that's not actually
      // needed for SMS.
      return UCS2_ENCODER.encode(CharBuffer.wrap(message)).remaining() - 1;
    }

    throw new IllegalArgumentException("No telecom encoder encodes message");
  }

  private static Stream<Arguments> messageFitsInSingleSMSSegment() throws URISyntaxException {
    final URL resourceUrl = Objects.requireNonNull(VerificationSmsBodyLengthTest.class.getResource("sms.properties"));
    final File resourceDirectory = Objects.requireNonNull(Paths.get(resourceUrl.toURI()).getParent().toFile());

    //noinspection ConstantConditions
    return Arrays.stream(resourceDirectory.listFiles(file ->
            file.getName().startsWith("sms") && file.getName().endsWith(".properties")))
        .flatMap(file -> {
          final Properties properties = new Properties();

          try (final FileReader fileReader = new FileReader(file)) {
            properties.load(fileReader);
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }

          final String languageTag =
              StringUtils.removeEnd(StringUtils.removeStart(file.getName(), "sms_"), ".properties")
                  .replace('_', '-');

          return properties.keySet().stream()
              .map(key -> Arguments.argumentSet("%s: %s".formatted(languageTag, key),
                  Locale.forLanguageTag(languageTag), key));
        });
  }
}
