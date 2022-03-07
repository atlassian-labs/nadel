package graphql.nadel.hooks;

import graphql.PublicApi;
import graphql.nadel.Service;

@PublicApi
public class CreateServiceContextParams {
    private final Service service;

    private CreateServiceContextParams(Builder builder) {
        this.service = builder.service;
    }

    public Service getService() {
        return service;
    }

    public static Builder newParameters() {
        return new Builder();
    }

    public static class Builder {
        private Service service;

        public Builder service(Service service) {
            this.service = service;
            return this;
        }

        public CreateServiceContextParams build() {
            return new CreateServiceContextParams(this);
        }
    }
}
