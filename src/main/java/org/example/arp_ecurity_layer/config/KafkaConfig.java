package org.example.arp_ecurity_layer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        // TODO: Configure acks=all, enable.idempotence=true, retries=MAX_VALUE, max.in.flight=5
        // TODO: Use StringSerializer for key and value
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        // TODO: Configure group-id from properties, StringDeserializer, auto-offset-reset=earliest
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        // TODO: Set AckMode.RECORD, wire DefaultErrorHandler with DeadLetterPublishingRecoverer
        throw new UnsupportedOperationException("TODO: implement");
    }
}
