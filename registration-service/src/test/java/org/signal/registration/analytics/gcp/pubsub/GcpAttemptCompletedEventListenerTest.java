package org.signal.registration.analytics.gcp.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.protobuf.InvalidProtocolBufferException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.signal.registration.rpc.ClientType;
import org.signal.registration.rpc.MessageTransport;
import org.signal.registration.session.FailedSendAttempt;
import org.signal.registration.session.FailedSendReason;
import org.signal.registration.session.RegistrationAttempt;
import org.signal.registration.session.RegistrationSession;
import org.signal.registration.session.SessionCompletedEvent;
import org.signal.registration.util.UUIDUtil;

class GcpAttemptCompletedEventListenerTest {

  private static final Phonenumber.PhoneNumber PHONE_NUMBER = PhoneNumberUtil.getInstance().getExampleNumber("US");
  private AttemptCompletedPubSubClient pubSubClient;
  private GcpAttemptCompletedEventListener listener;


  @BeforeEach
  void setUp() {
    final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    pubSubClient = mock(AttemptCompletedPubSubClient.class);
    listener = new GcpAttemptCompletedEventListener(meterRegistry, pubSubClient, MoreExecutors.directExecutor());
  }

  @ParameterizedTest
  @MethodSource
  void onApplicationEvent(final FailedSendReason failedSendReason, final boolean expectThatFailedAttemptIsPublished) {
    final FailedSendAttempt failedSendAttempt = FailedSendAttempt.newBuilder()
        .setClientType(ClientType.CLIENT_TYPE_ANDROID_WITH_FCM)
        .setFailedSendReason(failedSendReason)
        .setMessageTransport(MessageTransport.MESSAGE_TRANSPORT_SMS)
        .setSelectionReason("adaptive")
        .setSenderName("twilio-verify")
        .setTimestampEpochMillis(0L)
        .build();
    final RegistrationAttempt registrationAttempt = RegistrationAttempt.newBuilder()
        .setClientType(ClientType.CLIENT_TYPE_ANDROID_WITH_FCM)
        .setExpirationEpochMillis(1000L)
        .setRemoteId("remoteId")
        .setMessageTransport(MessageTransport.MESSAGE_TRANSPORT_SMS)
        .setSenderName("infobip")
        .setTimestampEpochMillis(0L)
        .build();
    final SessionCompletedEvent event = new SessionCompletedEvent(RegistrationSession.newBuilder()
        .setId(UUIDUtil.uuidToByteString(UUID.randomUUID()))
        .setPhoneNumber(PhoneNumberUtil.getInstance().format(PHONE_NUMBER, PhoneNumberUtil.PhoneNumberFormat.E164))
        .setVerifiedCode("VERIFIED")
        .addFailedAttempts(failedSendAttempt)
        .addRegistrationAttempts(registrationAttempt)
        .build());
    listener.onApplicationEvent(event);
    final ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(pubSubClient, times(expectThatFailedAttemptIsPublished ? 2 : 1)).send(argumentCaptor.capture());
    final List<CompletedAttemptPubSubMessage> pubSubMessages = argumentCaptor.getAllValues().stream()
        .map(data -> {
          try {
            return CompletedAttemptPubSubMessage.parseFrom(data);
          } catch (final InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
    final Iterator<CompletedAttemptPubSubMessage> iterator = pubSubMessages.iterator();
    int attemptId = 0;
    if (expectThatFailedAttemptIsPublished) {
      final CompletedAttemptPubSubMessage failedAttemptMessage = iterator.next();
      assertEquals(attemptId++, failedAttemptMessage.getAttemptId());
      assertFalse(failedAttemptMessage.getVerified());
    }
    final CompletedAttemptPubSubMessage succeededAttemptMessage = iterator.next();
    assertEquals(attemptId, succeededAttemptMessage.getAttemptId());
    assertTrue(succeededAttemptMessage.getVerified());
  }

  public static Stream<Arguments> onApplicationEvent() {
    return Stream.of(
        Arguments.of(FailedSendReason.FAILED_SEND_REASON_UNAVAILABLE, true),
        Arguments.of(FailedSendReason.FAILED_SEND_REASON_REJECTED, false),
        Arguments.of(FailedSendReason.FAILED_SEND_REASON_SUSPECTED_FRAUD, false)
    );
  }
}
