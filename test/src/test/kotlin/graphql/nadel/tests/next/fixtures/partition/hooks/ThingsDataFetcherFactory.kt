package graphql.nadel.tests.next.fixtures.partition.hooks

import graphql.schema.DataFetcher

object ThingsDataFetcherFactory {
    fun makeIdsDataFetcher(
        partitionsToThrowErrorFor: List<String> = emptyList(),
    ): DataFetcher<List<Map<String, String>>> {
        return DataFetcher { env ->
            val ids = env.getArgument<List<String>>("ids")!!

            if (ids.any { id -> partitionsToThrowErrorFor.any { partition -> id.contains(partition) } }) {
                throw RuntimeException("Error fetching things: $ids")
            }

            ids.map { id ->
                val parts = id.split(":")
                mapOf(
                    "id" to parts[0],
                    "name" to parts[0].uppercase(),
                    "age" to  "10"
                )
            }
        }
    }

    fun makeFilterDataFetcher(): DataFetcher<List<Map<String, String>>> {
        return DataFetcher { env ->
            val filter = env.getArgument<Map<String, List<Map<String, String>>>>("filter")!!
            val thingsIds = filter["thingsIds"]!!

            thingsIds.map { thingId ->
                val parts = thingId["id"]!!.split(":")
                mapOf(
                    "id" to parts[0],
                    "name" to parts[0].uppercase(),
                    "age" to  "10"
                )
            }
        }
    }
}
