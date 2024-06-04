package ru.springpendal.scrolling;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.query.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonRepository personRepository;

    private final EntityManager entityManager;

    @GetMapping("/by-lastname-offset/{lastname}")
    public Window<Person> findFirst10ByLastnameOffset(@PathVariable String lastname, @Nullable @RequestParam Long offset) {
        long start = System.currentTimeMillis();

        Window<Person> personWindow = personRepository.findFirst100ByLastnameOrderByFirstname(lastname, ScrollPosition.offset(offset != null ? offset : 0));

        log.info("offsetTime: " + (System.currentTimeMillis() - start));

        return personWindow;
    }

    @GetMapping("/traverse-lastname-offset/{lastname}")
    public long traverseOffset(@PathVariable String lastname) {
        long start = System.currentTimeMillis();

        ScrollPosition offset = ScrollPosition.offset();
        Window<Person> personWindow;
        do {
            personWindow = personRepository.findFirst100ByLastnameOrderByFirstname(lastname, offset);
            offset = personWindow.positionAt(personWindow.size() - 1);
        } while (personWindow.hasNext());

        long time = System.currentTimeMillis() - start;
        log.info("offsetTime: " + time);
        return time;
    }

    @GetMapping("/traverse-lastname-keyset/{lastname}")
    public void traverseKeySet(@PathVariable String lastname) {
        long start = System.currentTimeMillis();

        ScrollPosition offset = ScrollPosition.keyset();
        Window<Person> personWindow;
        do {
            personWindow = personRepository.findFirst100ByLastnameOrderByFirstname(lastname, offset);
            if (personWindow.hasNext()) {
                offset = personWindow.positionAt(personWindow.size() - 1);
            }
        } while (personWindow.hasNext());

        long time = System.currentTimeMillis() - start;
        log.info("keysetTime: " + time);

    }

    @GetMapping("/traverse-lastname-keyset-manual/{lastname}")
    public void traverseKeySetManual(@PathVariable String lastname) {
        long start = System.currentTimeMillis();

        Pageable pageable = Pageable.ofSize(100);

        Person lastPerson = null;
        List<Person> personWindow = personRepository.findPersonsByLastname(lastname, pageable);
        while (!personWindow.isEmpty()) {
            lastPerson = personWindow.get(personWindow.size() - 1);

            personWindow = personRepository.findPersonsByLastnameAndFirstnameAndId(
                lastname,
                lastPerson.getFirstname(),
                lastPerson.getId(),
                pageable
            );
        }


        long time = System.currentTimeMillis() - start;
        log.info("keysetManualTime: " + time);
    }

    @GetMapping("/traverse-lastname-keyset-hibernate/{lastname}")
    public void traverseKeySetHibernate(@PathVariable String lastname) {
        long start = System.currentTimeMillis();

        Session session = entityManager.unwrap(Session.class);

        Query<Person> query = session.createQuery("select p from Person p where p.lastname=:lastname", Person.class)
            .setParameter("lastname", lastname);

        KeyedPage<Person> keyedPage = Page.first(100)
            .keyedBy(List.of(
                Order.asc(Person.class, "firstname"),
                Order.asc(Person.class, "id")
            ));

        KeyedResultList<Person> keyedResultList;
        do {
            keyedResultList = query
                .getKeyedResultList(keyedPage);

            keyedPage = keyedResultList.getNextPage();
        } while (!keyedResultList.isLastPage());

        long time = System.currentTimeMillis() - start;
        log.info("keysetHibernateTime: " + time);
    }

    @GetMapping("/by-lastname-keyset/{lastname}")
    public Window<Person> findFirst100ByLastnameKeysetAfter(@PathVariable String lastname, @RequestParam @Nullable String firstname, @RequestParam @Nullable String id) {
        long start = System.currentTimeMillis();

        Window<Person> personWindow = personRepository.findFirst100ByLastnameOrderByFirstname(
            lastname,
            firstname != null && id != null ?
                ScrollPosition.of(Map.of("firstname", firstname, "id", id), ScrollPosition.Direction.FORWARD) :
                ScrollPosition.keyset()
        );

        log.info("keySetTime:" + (System.currentTimeMillis() - start));
        return personWindow;
    }
}
