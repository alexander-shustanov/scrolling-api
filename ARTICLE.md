## Пагинация в Spring Data: Прошлое и настоящее

В Spring Data 3.1 появилось новое API для работой с большими объемами данных: Scrolling API. Давайте изучим его и посмотрим на его преимущества по сравнению со старым добрым Pageable.

Работа с API происходит через Derived Query Method, и тут мы видимо первое большое отличие от изначального подхода с Pageable. Размер страницы жестко задается в самом методе и не может быть явно передан в качестве параметра запроса:

```java
Window<Person> findFirst10ByLastnameOrderByFirstname(String lastname, ScrollPosition scrollPosition);
```

Сразу видим два новых класса: `Window` и `ScrollPosition`. Window хранит результат, а ScrollPosition определяет точку, с которой мы хотим загрузить данные.

### Window

Поглядим поближе на Window. Интерфейс похож на Pageable/Slice за исключением того, что вместо методов для получения следующей страницы, имеются методы для получения ScrollPosition некоторого элемента.

```java
public interface Window<T> extends Streamable<T> {
    int size();

    boolean isEmpty();

    List<T> getContent();

    default boolean isLast() {
        ...
    }

    boolean hasNext();

    ScrollPosition positionAt(int index);

    default ScrollPosition positionAt(T object) {
        ...
    }

    <U> Window<U> map(Function<? super T, ? extends U> converter);
}
```

Лично мне, сходу не понятно, почему нельзя было сделать метод получения ScrollPosition сразу для последнего загруженного элемента, тем более что в примере в самой документации сценарий использования window.positionAt именно такой:
```java
Window<User> users = repository.findFirst10ByLastnameOrderByFirstname("Doe", ScrollPosition.offset());

users = repository.findFirst10ByLastnameOrderByFirstname("Doe", users.positionAt(users.size() - 1));
```

### ScrollPosition

ScrollPosition бывает двух видов: offset и keyset. Offset фактически работает также как и Pageable: за счет добавления OFFSET в запрос к базе. KeySet работает несколько интереснее, и модифицирует запрос, чтобы откинуть уже полученные данные. Такой подход может дать рост производительности, так как обычная OFFSET пагинация требует повторного прохода по всем ранее загруженным данным. Таким образом, интерфейс имеет несколько фабричных методов для содания offset или keyset ScrollingPosition. Кроме того, keyset пагинация (кажется, ее можно назвать курсорной), не чувствительна к вставке и удалению уже просмотренных данных.

```java
public interface ScrollPosition {
    static KeysetScrollPosition keyset() {
        ...
    }

    static OffsetScrollPosition offset() {
        ...
    }

    static OffsetScrollPosition offset(long offset) {
        ...
    }

    static KeysetScrollPosition forward(Map<String, ?> keys) {
        ...
    }

    static KeysetScrollPosition backward(Map<String, ?> keys) {
        ...
    }

    static KeysetScrollPosition of(Map<String, ?> keys, Direction direction) {
        ...
    }
}
```

### Проверяем в бою

Давайте посмотрим, как работают каждая их этих пагинаций под капотом. Включим логирование sql запросов с помощью проперти:

```properties
spring.jpa.show-sql=true
```

Тестировать будем такой вот метод:

```java
public interface PersonRepository extends JpaRepository<Person, Long> {
    Window<Person> findFirst10ByLastnameOrderByFirstname(String lastname, ScrollPosition scrollPosition);
}
```

Сначала передадим `ScrollPosition.offset()`:

```java
Window<Person> personWindow = personRepository.findFirst10ByLastnameOrderByFirstname(lastname, ScrollPosition.offset());
```

Видим в логах вот такой запрос:

```sql
select p1_0.id,p1_0.age,p1_0.firstname,p1_0.lastname from person p1_0 where p1_0.lastname=? order by p1_0.firstname offset ? rows fetch first ? rows only
```

Делаем загрузку следующей страницы, и получаем буквально такой же запрос в логах. Но, другого мы и не ожидали, принцип такой же, как был в Pageable. 

Давайте посмотрим, как обстоят дела с KeySet. После начального запроса загрузим сразу вторую страницу.

```java
Window<Person> personWindow = personRepository.findFirst10ByLastnameOrderByFirstname(lastname, ScrollPosition.keyset());

ScrollPosition scrollPosition = personWindow.positionAt(personWindow.size() - 1);
personWindow = personRepository.findFirst10ByLastnameOrderByFirstname(lastname, scrollPosition);
```

Смотрим в логи, и тут видим кое-что по-интересней.

```sql
select p1_0.id,p1_0.age,p1_0.firstname,p1_0.lastname from person p1_0 where p1_0.lastname=? order by p1_0.firstname,p1_0.id fetch first ? rows only

select p1_0.id,p1_0.age,p1_0.firstname,p1_0.lastname from person p1_0 where p1_0.lastname=? and (p1_0.firstname>? or p1_0.firstname=? and p1_0.id>?) order by p1_0.firstname,p1_0.id fetch first ? rows only
```

Замечаем, что у нас изменился набор полей в order by. Зачем это нужно можно понять, посмотрев на второй запрос, который отличается от первого дополнительным условием в where. Какие параметры будут переданны в этот запрос? Можно ответить на этот вопрос, посмотрев на содержимое ScrollingPosition перед его исполнением.

![keysey_scrolling_position.png](media%2Fkeysey_scrolling_position.png)

Фактически, это значения полей из order by последней загруженной записи. Сразу становится понятно, почему был добавлен id, и как работает курсорная пагинация.

### Ограничения Keyset пагинации

Курсорная пагинация имеет ряд ограничений. Первое ограничение - отсутствие номера страницы. Мы не можем грузить данные с какого-то произвольного места, и так или иначе, нам придется последовательно перебирать их. Но, справедливости ради, в offset пагинации это так или иначе происходит, и при этом сильно медленнее.

Во-вторых, желательно иметь индексы на поля сортировки, иначе увеличения производительности не видать. 

И, на последок, комбинации значений полей, по которым происходит сортировка, должны быть уникальные, но Spring может тут справиться и без разработчика, просто добавив id. 

### Оставшиеся вопросы

Я не нашел предпочтительный способ управления скроллингом c клиента. Если вернуть в REST Controller Window, то ScrollPosition мы в нем не увидим. Я нашел класс OffsetScrollPositionHandlerMethodArgumentResolver для парсинга OffsetScrollPosition в качестве параметра метода контроллера. Но, не нашел ничего подобного для KeySetScrollPosition, так что реализация тут полностью ложится на плечи разработчика. Но принцип будет такой, что с клиента должен прийти последний загруженный объект, после которого мы хотим грузить остальные. И конструировать ScrollPosition надо будет вручную.

```java
@GetMapping("/by-lastname-keyset/{lastname}")
public Window<Person> findFirst10ByLastnameKeysetAfter(@PathVariable String lastname, @Nullable @ModelAttribute Person lastLoaded) {
    return personRepository.findFirst10ByLastnameOrderByFirstname(
        lastname,
        lastLoaded != null ?
            ScrollPosition.of(Map.of("firstname", lastLoaded.getFirstname(), "id", lastLoaded.getId()), ScrollPosition.Direction.FORWARD) :
                ScrollPosition.keyset()
        );
}
```
