package ru.springpendal.scrolling;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonRepository personRepository;

    @GetMapping("/by-lastname-offset/{lastname}")
    public Window<Person> findFirst10ByLastnameOffset(@PathVariable String lastname, @Nullable @RequestParam Long offset) {
        long start = System.currentTimeMillis();

        Window<Person> personWindow = personRepository.findFirst100ByLastnameOrderByFirstname(lastname, ScrollPosition.offset(offset != null ? offset : 0));

        System.out.println("offsetTime: " + (System.currentTimeMillis() - start));

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
        System.out.println("offsetTime: " + time);
//        offsetTime: 350119
        return time;
    }

    @GetMapping("/traverse-lastname-keyset/{lastname}")
    public long traverseKeySet(@PathVariable String lastname) {
        long start = System.currentTimeMillis();

        ScrollPosition offset = ScrollPosition.keyset();
        Window<Person> personWindow;
        do {
            personWindow = personRepository.findFirst100ByLastnameOrderByFirstname(lastname, offset);
            offset = personWindow.positionAt(personWindow.size() - 1);
        } while (personWindow.hasNext());

        long time = System.currentTimeMillis() - start;
        System.out.println("keysetTime: " + time);

        return time;
    }

    @GetMapping("/by-lastname-keyset/{lastname}")
    public Window<Person> findFirst10ByLastnameKeysetAfter(@PathVariable String lastname, @RequestParam @Nullable String firstname, @RequestParam @Nullable String id) {
        long start = System.currentTimeMillis();

        Window<Person> first10ByLastnameOrderByFirstname = personRepository.findFirst100ByLastnameOrderByFirstname(
            lastname,
            firstname != null && id != null ?
                ScrollPosition.of(Map.of("firstname", firstname, "id", id), ScrollPosition.Direction.FORWARD) :
                ScrollPosition.keyset()
        );

        System.out.println("keySetTime:" + (System.currentTimeMillis() - start));
        return first10ByLastnameOrderByFirstname;
    }
}


