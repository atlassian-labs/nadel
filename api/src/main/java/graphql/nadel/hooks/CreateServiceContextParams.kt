package graphql.nadel.hooks

import graphql.nadel.Service

// todo make constructor internal once we merge api/ and engine-nextgen/
data class CreateServiceContextParams constructor(val service: Service)
