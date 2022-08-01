package com.library.consumer.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.consumer.consumer.LibraryEventsConsumer;
import com.library.consumer.entity.Book;
import com.library.consumer.entity.LibraryEvent;
import com.library.consumer.entity.LibraryEventType;
import com.library.consumer.repository.LibraryEventsRepository;
import com.library.consumer.service.LibraryEventsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = "library-events", partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class LibraryConsumerTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @SpyBean
    LibraryEventsConsumer libraryEventsConsumer;

    @SpyBean
    LibraryEventsService libraryEventsService;

    @Autowired
    LibraryEventsRepository libraryEventsRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @Test
    void newLibraryEventTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String json = "{\"id\":null,\"libraryEventType\":\"NEW\",\"book\":{\"id\":\"Book-1\",\"name\":\"Harry Potter\",\"author\":\"J.K Rowling\"}}";
        kafkaTemplate.sendDefault(json).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        assert libraryEventsRepository.findAll().size() == 1;

        libraryEventsRepository.findAll().forEach(libraryEvent -> {
            assert libraryEvent.getId() != null;
            assertEquals("Book-1", libraryEvent.getBook().getId());
        });
    }

    @Test
    void updateLibraryEventTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String json = "{\"id\":null,\"libraryEventType\":\"NEW  \",\"book\":{\"id\":\"Book-1\",\"name\":\"Harry Potter\",\"author\":\"J.K Rowling\"}}";
        LibraryEvent libraryEvent = objectMapper.readValue(json,LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);

        Book updatedBook = Book.builder()
                .id("Book-1")
                .name("Harry Potter and The Chamber of Secrets")
                .author("J.K Rowling")
                .build();
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.setBook(updatedBook);

        String updatedJson = objectMapper.writeValueAsString(libraryEvent);

        kafkaTemplate.sendDefault(libraryEvent.getId(),updatedJson).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        Optional<LibraryEvent> libraryEventOptional = libraryEventsRepository.findById(libraryEvent.getId());
        assertEquals("Harry Potter and The Chamber of Secrets",libraryEventOptional.get().getBook().getName());

    }

    @Test
    void updateLibraryEventExceptionTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String json = "{\"id\":null,\"libraryEventType\":\"UPDATE\",\"book\":{\"id\":\"Book-1\",\"name\":\"Harry Potter\",\"author\":\"J.K Rowling\"}}";

        kafkaTemplate.sendDefault(json).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);

        verify(libraryEventsConsumer, times(10)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsService, times(10)).processLibraryEvent(isA(ConsumerRecord.class));
    }

    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
    }
}
