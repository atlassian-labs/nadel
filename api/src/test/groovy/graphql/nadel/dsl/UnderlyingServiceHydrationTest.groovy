package graphql.nadel.dsl

import graphql.AssertException
import graphql.language.SourceLocation
import spock.lang.Specification

class UnderlyingServiceHydrationTest extends Specification {
    def "error if providing an identifier and the object indexed is null"() {
        when:
        new UnderlyingServiceHydration(
                new SourceLocation(0, 0),
                Collections.emptyList(),
                "Service",
                "Query",
                "test",
                Collections.emptyList(),
                "test",
                Collections.emptyList(),
                true,
                false,
                5,
                -1,
                Collections.emptyMap()
        )

        then:
        AssertException ex = thrown()

        ex.message == "An object identifier cannot be provided if the hydration is by index"
    }

    def "no error thrown if indexed with an identifier"() {
        when:
        new UnderlyingServiceHydration(
                new SourceLocation(0, 0),
                Collections.emptyList(),
                "Service",
                "Query",
                "test",
                Collections.emptyList(),
                "test",
                Collections.emptyList(),
                false,
                false,
                5,
                -1,
                Collections.emptyMap()
        )

        then:
        notThrown AssertException
    }

    def "no error thrown if using index"() {
        when:
        new UnderlyingServiceHydration(
                new SourceLocation(0, 0),
                Collections.emptyList(),
                "Service",
                "Query",
                "test",
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                true,
                false,
                5,
                -1,
                Collections.emptyMap()
        )

        then:
        notThrown AssertException
    }
}
