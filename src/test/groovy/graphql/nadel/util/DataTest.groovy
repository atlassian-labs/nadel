package graphql.nadel.util

import graphql.AssertException
import spock.lang.Specification

import java.sql.Timestamp
import java.time.Instant

class DataTest extends Specification {

    def now = new Date()
    def then = Instant.now()
    def ts = new Timestamp(System.currentTimeMillis())

    def "basic setting of data"() {
        when:
        def data = Data.newData("x", 1, now).build()
        then:
        data.get(String.class) == "x"
        data.get(Integer.class) == 1
        data.get(Date.class) == now
    }

    def "by name setting of data"() {
        def now = new Date()
        when:
        def data = Data.newData("x", 1, now)
                .set("then", then)
                .set(Timestamp.class, ts)
                .build()
        then:
        data.get(String.class) == "x"
        data.get(Integer.class) == 1
        data.get(Date.class) == now
        data.get("then") == then
        data.get(Timestamp.class) == ts
    }

    def "single shot value"() {
        when:
        def data = Data.of("single")
        then:
        data.get(String.class) == "single"

        when:
        data = Data.newData("single").build()
        then:
        data.get(String.class) == "single"
    }

    def "multiples values"() {
        when:
        def data = Data.of("single", 99, 666L)
        then:
        data.get(String.class) == "single"
        data.get(Integer.class) == 99
        data.get(Long.class) == 666L

        when:
        data = Data.newData("single", 99, 666L).build()
        then:
        data.get(String.class) == "single"
        data.get(Integer.class) == 99
        data.get(Long.class) == 666L
    }

    def "null handling"() {
        when:
        Data.of(null)
        then:
        thrown(AssertException.class)

        when:
        Data.of("x", null, "y")
        then:
        thrown(AssertException.class)
    }

    def "can have null values however by name"() {
        when:
        def data = Data.newData().set("key1", null).set(String.class, null).build()
        then:
        data.get("key1") == null
        data.get(String.class) == null
    }

    def "defaulting works"() {
        when:
        def emptyData = Data.newData().build()
        then:
        emptyData.getOrDefault(String.class, "default1") == "default1"
        emptyData.getOrDefault(Integer.class, 1) == 1
        emptyData.getOrDefault("byKey", "default3") == "default3"
    }

    def "defaulting with wrong class is detected"() {
        when:
        def emptyData = Data.newData().build()
        emptyData.getOrDefault(Integer.class, "default1")
        then:
        thrown(AssertException)
    }

    def "asMap works"() {
        when:
        def data = Data.of("s", 1, true)
        def map = data.asMap()
        then:
        map == ["java.lang.String" : "s",
                "java.lang.Integer": 1,
                "java.lang.Boolean": true
        ]
    }

    def "generic containers kinda work via hard coded remapping"() {
        List<String> genericList = new ArrayList<>()
        Set<String> genericSet = new HashSet<>()
        Map<String, Object> genericMap = new HashMap<>()

        when:
        def data = Data.of(genericList, genericSet, genericMap)
        then:
        data.get(List.class) == genericList
        data.get(Set.class) == genericSet
        data.get(Map.class) == genericMap
    }
}
