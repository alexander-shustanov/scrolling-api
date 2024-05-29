package ru.springpendal.scrolling;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public Window<Person> findFirst10ByLastnameOrderByFirstname(String lastname, ScrollPosition scrollPosition) {
        return personRepository.findFirst100ByLastnameOrderByFirstname(lastname, scrollPosition);
    }
}
