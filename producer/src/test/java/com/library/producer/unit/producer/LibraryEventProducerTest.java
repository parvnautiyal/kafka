package com.library.producer.unit.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.producer.domain.LibraryEvent;
import com.library.producer.entity.Book;
import com.library.producer.producer.LibraryEventProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryEventProducerTest {

    @Mock
    KafkaTemplate<Integer,String> kafkaTemplate;

    @Spy
    ObjectMapper objectMapper;

    @InjectMocks
    LibraryEventProducer libraryEventProducer;


    @Test
    void sendLibraryEventFailTest() throws ExecutionException, InterruptedException {
        Book book = Book.builder()
                .id("Book1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        SettableListenableFuture future = new SettableListenableFuture();
        future.setException(new RuntimeException("Exception calling kafka"));

        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

        assertThrows(Exception.class,()->libraryEventProducer.sendLibraryEventApproachTwo(libraryEvent).get());
    }

    @Test
    void sendLibraryEventSuccessTest() throws JsonProcessingException, ExecutionException, InterruptedException {
        Book book = Book.builder()
                .id("Book1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        SettableListenableFuture future = new SettableListenableFuture();

        String record = objectMapper.writeValueAsString(libraryEvent);
        ProducerRecord<Integer,String> producerRecord = new ProducerRecord("library-events",libraryEvent.getId(),record);

        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition("library-events",1),1,1,System.currentTimeMillis(),1,2);

        SendResult<Integer,String> sendResult = new SendResult<>(producerRecord,recordMetadata);

        future.set(sendResult);

        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

        ListenableFuture<SendResult<Integer,String>> listenableFuture = libraryEventProducer.sendLibraryEventApproachTwo(libraryEvent);

        SendResult<Integer,String> sendResult1 = listenableFuture.get();

        assert sendResult1.getRecordMetadata().partition() == 1;
    }
}
