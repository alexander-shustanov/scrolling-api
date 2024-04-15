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

//    @GetMapping("/by-lastname-keyset/{lastname}")
//    public Window<Person> findFirst10ByLastnameKeyset(@PathVariable String lastname) {
//        Window<Person> personWindow = personRepository.findFirst10ByLastnameOrderByFirstname(lastname, ScrollPosition.keyset());
//
//        ScrollPosition scrollPosition = personWindow.positionAt(personWindow.size() - 1);
//        personRepository.findFirst10ByLastnameOrderByFirstname(lastname, scrollPosition);
//        return personWindow;
//    }

    @GetMapping("/by-lastname-offset/{lastname}")
    public Window<Person> findFirst10ByLastnameOffset(@PathVariable String lastname) {
        Window<Person> personWindow = personRepository.findFirst10ByLastnameOrderByFirstname(lastname, ScrollPosition.offset());

        ScrollPosition scrollPosition = personWindow.positionAt(personWindow.size() - 1);
        personRepository.findFirst10ByLastnameOrderByFirstname(lastname, scrollPosition);
        return personWindow;
    }

    @GetMapping("/by-lastname-keyset/{lastname}")
    public Window<Person> findFirst10ByLastnameKeysetAfter(@PathVariable String lastname, @Nullable @ModelAttribute Person lastLoaded) {
        return personRepository.findFirst10ByLastnameOrderByFirstname(
            lastname,
            lastLoaded != null ?
                ScrollPosition.of(Map.of("firstname", lastLoaded.getFirstname(), "id", lastLoaded.getId()), ScrollPosition.Direction.FORWARD) :
                ScrollPosition.keyset()
        );
    }
}
