package com.library.producer.integration;

import com.library.producer.domain.LibraryEvent;
import com.library.producer.entity.Book;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = "library-events",partitions = 3)
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
class LibraryControllerIntegrationTest {

    private Consumer<Integer,String> consumer;
    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void setup(){
        Map<String,Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1","true",embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(configs,new IntegerDeserializer(),new StringDeserializer()).createConsumer();
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @Test
    @Timeout(5)
    void postLibraryEventTest() throws InterruptedException {
        Book book = Book.builder()
                .id("Book-1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent,httpHeaders);

        ResponseEntity<LibraryEvent> response = restTemplate.exchange("/api/v1.0/libraryEvent", HttpMethod.POST,request,LibraryEvent.class);

        assertEquals(HttpStatus.CREATED,response.getStatusCode());

        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);
//        Thread.sleep(3000);

        assert consumerRecords.count() == 1;
        consumerRecords.forEach(record-> {
            String expectedRecord = "{\"id\":null,\"libraryEventType\":\"NEW\",\"book\":{\"id\":\"Book-1\",\"name\":\"Harry Potter\",\"author\":\"J.K Rowling\"}}";
            String value = record.value();
            assertEquals(expectedRecord, value);
        });
    }

    @Test
    @Timeout(5)
    void putLibraryEventTest() {
        Book book = Book.builder()
                .id("Book-1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(123)
                .book(book)
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent,httpHeaders);

        ResponseEntity<LibraryEvent> response = restTemplate.exchange("/api/v1.0/libraryEvent", HttpMethod.PUT,request,LibraryEvent.class);

        assertEquals(HttpStatus.OK,response.getStatusCode());

        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

        assert consumerRecords.count() == 2;
        consumerRecords.forEach(record-> {
            if(record.key()!=null){
                String expectedRecord = "{\"id\":123,\"libraryEventType\":\"UPDATE\",\"book\":{\"id\":\"Book-1\",\"name\":\"Harry Potter\",\"author\":\"J.K Rowling\"}}";
                String value = record.value();
                assertEquals(expectedRecord, value);
            }
        });
    }

    @AfterEach
    void tearDown(){
        consumer.close();

    }
}
