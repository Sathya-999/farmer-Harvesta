package com.agriplatform.notification;

import com.agriplatform.notification.dto.CreateNotificationRequest;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 11 ? Notification DTO Tests")
class NotificationDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("NotificationDto record")
    class NotificationDtoRecordTests {

        @Test
        @DisplayName("Record fields accessible correctly")
        void recordFields_accessible() {
            UUID id = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            NotificationDto dto = new NotificationDto(id, "Title", "Body", "GENERAL", false, null, now);
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.title()).isEqualTo("Title");
            assertThat(dto.body()).isEqualTo("Body");
            assertThat(dto.type()).isEqualTo("GENERAL");
            assertThat(dto.read()).isFalse();
            assertThat(dto.readAt()).isNull();
            assertThat(dto.createdAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Read=true when readAt is set")
        void readTrue_whenReadAtSet() {
            NotificationDto dto = new NotificationDto(
                UUID.randomUUID(), "T", "B", "SYSTEM", true, OffsetDateTime.now(), OffsetDateTime.now());
            assertThat(dto.read()).isTrue();
            assertThat(dto.readAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("UnreadCountDto record")
    class UnreadCountDtoTests {

        @Test
        @DisplayName("Count is preserved correctly")
        void count_preserved() {
            UnreadCountDto dto = new UnreadCountDto(42L);
            assertThat(dto.unreadCount()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Zero count is valid")
        void zeroCount_valid() {
            UnreadCountDto dto = new UnreadCountDto(0L);
            assertThat(dto.unreadCount()).isZero();
        }
    }

    @Nested
    @DisplayName("CreateNotificationRequest validation")
    class CreateRequestValidationTests {

        @Test
        @DisplayName("Valid request passes validation")
        void validRequest_passes() {
            CreateNotificationRequest req = new CreateNotificationRequest("Title", "Body", "GENERAL");
            Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Blank title fails validation")
        void blankTitle_fails() {
            CreateNotificationRequest req = new CreateNotificationRequest("  ", "Body", "GENERAL");
            Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
        }

        @Test
        @DisplayName("Blank body fails validation")
        void blankBody_fails() {
            CreateNotificationRequest req = new CreateNotificationRequest("Title", "", "GENERAL");
            Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("body");
        }

        @Test
        @DisplayName("Null type is allowed (service defaults to GENERAL)")
        void nullType_allowed() {
            CreateNotificationRequest req = new CreateNotificationRequest("Title", "Body", null);
            Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Title exceeding 255 chars fails validation")
        void titleTooLong_fails() {
            String longTitle = "A".repeat(256);
            CreateNotificationRequest req = new CreateNotificationRequest(longTitle, "Body", null);
            Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }
    }
}