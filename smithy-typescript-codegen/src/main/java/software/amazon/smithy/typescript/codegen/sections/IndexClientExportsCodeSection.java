/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.typescript.codegen.sections;

import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public class IndexClientExportsCodeSection implements CodeSection {
    private final ServiceShape service;

    private IndexClientExportsCodeSection(Builder builder) {
        service = SmithyBuilder.requiredState("service", builder.service);
    }

    public ServiceShape getService() {
        return service;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements SmithyBuilder<IndexClientExportsCodeSection> {
        private ServiceShape service;

        @Override
        public IndexClientExportsCodeSection build() {
            return new IndexClientExportsCodeSection(this);
        }

        public Builder service(ServiceShape service) {
            this.service = service;
            return this;
        }
    }
}
