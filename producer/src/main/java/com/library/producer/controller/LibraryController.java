package com.library.producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.library.producer.domain.LibraryEvent;
import com.library.producer.domain.LibraryEventType;
import com.library.producer.producer.LibraryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(("/api/v1.0"))
@Slf4j
public class LibraryController {

    @Autowired
    LibraryEventProducer libraryEventProducer;

    @PostMapping("/libraryEvent")
    public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException {
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);
            log.info("before send library event");
//            libraryEventProducer.sendLibraryEvent(libraryEvent);
//            SendResult<Integer,String> sendResult = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);
            libraryEventProducer.sendLibraryEventApproachTwo(libraryEvent);
//            log.info("Send Result is {}",sendResult.toString());
            log.info("after send library event");
            return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
    }

    @PutMapping("/libraryEvent")
    public ResponseEntity<?> putLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException {
        if(libraryEvent.getId()==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("please pass the id");
        }
        else {
            libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
            libraryEventProducer.sendLibraryEventApproachTwo(libraryEvent);
            return ResponseEntity.status(HttpStatus.OK).body(libraryEvent);
        }
    }
}
