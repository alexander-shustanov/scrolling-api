package ru.springpendal.scrolling;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Window<Person> findFirst10ByLastnameOrderByFirstname(String lastname, ScrollPosition scrollPosition);

    Window<Person> findFirst5ByLastname(String lastname, ScrollPosition scrollPosition);
}
