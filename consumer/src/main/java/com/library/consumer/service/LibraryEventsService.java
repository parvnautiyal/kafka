package com.library.consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.consumer.entity.LibraryEvent;
import com.library.consumer.repository.LibraryEventsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class LibraryEventsService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private LibraryEventsRepository libraryEventsRepository;

    public void processLibraryEvent(ConsumerRecord<Integer,String> consumerRecord) throws JsonProcessingException {
        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);
        log.info("library event: {}",libraryEvent);
        switch (libraryEvent.getLibraryEventType()){
            case NEW -> save(libraryEvent);
            case UPDATE -> {
                validate(libraryEvent);
                save(libraryEvent);
            }
            default -> log.info("invalid library event type");
        }
    }

    private void validate(LibraryEvent libraryEvent) {
        if(libraryEvent.getId() == null)
            throw new IllegalArgumentException("library event id missing");
        else{
            Optional<LibraryEvent> libraryEventOptional = libraryEventsRepository.findById(libraryEvent.getId());
            if(libraryEventOptional.isEmpty())
                throw new IllegalArgumentException("not a valid library event");
            else{
                log.info("validation is successful for event: {}", libraryEventOptional.get());
            }
        }
    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);
        log.info("successfully persisted the library event for id: {}",libraryEvent.getId());
    }
}
