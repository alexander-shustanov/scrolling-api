package ru.springpendal.scrolling;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Window<Person> findFirst100ByLastnameOrderByFirstname(String lastname, ScrollPosition scrollPosition);

    @Query("SELECT p FROM Person p WHERE p.lastname = :lastname AND (p.firstname >= :firstname AND (p.firstname > :firstname OR p.id > :id)) ORDER BY p.firstname, p.id")
    List<Person> findPersonsByLastnameAndFirstnameAndId(
        @Param("lastname") String lastname,
        @Param("firstname") String firstname,
        @Param("id") Long id,
        Pageable pageable);


    @Query("SELECT p FROM Person p WHERE p.lastname = :lastname ORDER BY p.firstname, p.id")
    List<Person> findPersonsByLastname(
        @Param("lastname") String lastname,
        Pageable pageable);

    Window<Person> findFirst5ByLastname(String lastname, ScrollPosition scrollPosition);
}
