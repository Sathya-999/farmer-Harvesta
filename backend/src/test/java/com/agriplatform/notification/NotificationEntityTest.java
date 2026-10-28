package com.agriplatform.notification;

import com.agriplatform.notification.entity.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 11 ? Notification Entity Tests")
class NotificationEntityTest {

    @Nested
    @DisplayName("Construction and defaults")
    class ConstructionTests {

        @Test
        @DisplayName("Constructor sets all fields correctly")
        void constructor_setsFields() {
            UUID farmerId = UUID.randomUUID();
            Notification n = new Notification(farmerId, "Test Title", "Test body", "CULTIVATION");
            assertThat(n.getFarmerId()).isEqualTo(farmerId);
            assertThat(n.getTitle()).isEqualTo("Test Title");
            assertThat(n.getBody()).isEqualTo("Test body");
            assertThat(n.getType()).isEqualTo("CULTIVATION");
            assertThat(n.getReadAt()).isNull();
            assertThat(n.isRead()).isFalse();
        }

        @Test
        @DisplayName("Null type defaults to GENERAL")
        void nullType_defaultsToGeneral() {
            Notification n = new Notification(UUID.randomUUID(), "T", "B", null);
            assertThat(n.getType()).isEqualTo(Notification.TYPE_GENERAL);
        }

        @Test
        @DisplayName("Blank type defaults to GENERAL")
        void blankType_defaultsToGeneral() {
            Notification n = new Notification(UUID.randomUUID(), "T", "B", "   ");
            assertThat(n.getType()).isEqualTo(Notification.TYPE_GENERAL);
        }

        @Test
        @DisplayName("TYPE constants are correct")
        void typeConstants_areCorrect() {
            assertThat(Notification.TYPE_GENERAL).isEqualTo("GENERAL");
            assertThat(Notification.TYPE_CULTIVATION).isEqualTo("CULTIVATION");
            assertThat(Notification.TYPE_SYSTEM).isEqualTo("SYSTEM");
            assertThat(Notification.TYPE_ANALYTICS).isEqualTo("ANALYTICS");
        }
    }

    @Nested
    @DisplayName("Read state")
    class ReadStateTests {

        @Test
        @DisplayName("isRead() is false when readAt is null")
        void isRead_false_whenReadAtNull() {
            Notification n = new Notification(UUID.randomUUID(), "T", "B", "GENERAL");
            assertThat(n.isRead()).isFalse();
            assertThat(n.getReadAt()).isNull();
        }
    }
}