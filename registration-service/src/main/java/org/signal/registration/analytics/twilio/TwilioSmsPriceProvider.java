/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.analytics.twilio;

import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.pricing.v1.messaging.Country;
import com.twilio.type.InboundSmsPrice;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.stream.Collectors;
import jakarta.inject.Singleton;
import org.signal.registration.analytics.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
class TwilioSmsPriceProvider {

  private static final Logger log = LoggerFactory.getLogger(TwilioSmsPriceProvider.class);
  private final TwilioRestClient twilioRestClient;

  TwilioSmsPriceProvider(final TwilioRestClient twilioRestClient) {
    this.twilioRestClient = twilioRestClient;
  }

  /**
   * Retrieves fresh pricing data from Twilio.
   *
   * @return a reactive stream of pricing data
   */
  Flux<TwilioSmsPrice> getPricingData() {
    return ReaderUtil.readerToFlux(Country.reader(), twilioRestClient)
        .map(Country::getIsoCountry)
        .flatMap(isoCountry -> Mono
            .fromFuture(Country.fetcher(isoCountry).fetchAsync(twilioRestClient))
            .onErrorResume(ApiException.class, e -> {
              log.warn("Failed to fetch price for {}, won't update price", isoCountry, e);
              return Mono.empty();
            }))
        .flatMap(country -> Flux.fromIterable(country.getOutboundSmsPrices())
            .map(outboundSmsPrice -> {
              final String region = country.getIsoCountry();
              final String mcc = outboundSmsPrice.getMcc();
              final String mnc = outboundSmsPrice.getMnc();

              final EnumMap<InboundSmsPrice.Type, Money> pricesByNumberType = new EnumMap<>(
                  outboundSmsPrice.getPrices().stream()
                      .collect(Collectors.toMap(
                          InboundSmsPrice::getType,
                          inboundSmsPrice -> new Money(BigDecimal.valueOf(inboundSmsPrice.getCurrentPrice()),
                              country.getPriceUnit()))
                      ));

              return new TwilioSmsPrice(region, mcc, mnc, pricesByNumberType);
            }));
  }
}
