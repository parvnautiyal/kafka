package com.library.consumer.repository;

import com.library.consumer.entity.LibraryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryEventsRepository extends JpaRepository<LibraryEvent,Integer> {
}
