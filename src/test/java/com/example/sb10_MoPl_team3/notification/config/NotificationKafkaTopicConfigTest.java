package com.example.sb10_MoPl_team3.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationKafkaTopicConfigTest {

    NotificationKafkaTopicConfig config = new NotificationKafkaTopicConfig();

    @Test
    void notificationFanoutTopic_usesConfiguredPartitionsAndReplicas() {
        var topic = config.notificationFanoutTopic(3, 1);

        assertThat(topic.name()).isEqualTo(NotificationKafkaTopics.FANOUT);
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void notificationFanoutDlqTopic_usesConfiguredPartitionsAndReplicas() {
        var topic = config.notificationFanoutDlqTopic(1, 1);

        assertThat(topic.name()).isEqualTo(NotificationKafkaTopics.FANOUT_DLQ);
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }
}
