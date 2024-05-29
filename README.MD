

test offset
```bash
curl "localhost:8080/person/traverse-lastname-offset/Smith"
```

test keyset
```bash
curl "localhost:8080/person/traverse-lastname-keyset/Smith"
```

query for keyset:

```sql
select p1_0.id,p1_0.age,p1_0.firstname,p1_0.lastname from person p1_0 where p1_0.lastname=? and (p1_0.firstname>? or p1_0.firstname=? and p1_0.id>?) order by p1_0.firstname,p1_0.id fetch first ? rows only
```

the same but effective:

```sql
select p1_0.id,p1_0.age,p1_0.firstname,p1_0.lastname from person p1_0 where p1_0.lastname=? and (p1_0.firstname>=? and (p1_0.firstname>? or p1_0.id>?)) order by p1_0.firstname,p1_0.id fetch first ? rows only;
```
