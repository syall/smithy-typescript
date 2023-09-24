/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.typescript.codegen.endpointsV2.integration;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.typescript.codegen.TypeScriptCodegenContext;
import software.amazon.smithy.typescript.codegen.TypeScriptDependency;
import software.amazon.smithy.typescript.codegen.TypeScriptWriter;
import software.amazon.smithy.typescript.codegen.endpointsV2.EndpointsParamNameMap;
import software.amazon.smithy.typescript.codegen.endpointsV2.EndpointsV2Generator;
import software.amazon.smithy.typescript.codegen.endpointsV2.RuleSetParameterFinder;
import software.amazon.smithy.typescript.codegen.integration.RuntimeClientPlugin;
import software.amazon.smithy.typescript.codegen.integration.TypeScriptIntegration;
import software.amazon.smithy.typescript.codegen.sections.CommandPropertiesCodeSection;
import software.amazon.smithy.typescript.codegen.sections.IndexClientExportsCodeSection;
import software.amazon.smithy.typescript.codegen.sections.SmithyContextCodeSection;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class AddEndpointRuleSetIntegration implements TypeScriptIntegration {
    @Override
    public List<RuntimeClientPlugin> getClientPlugins() {
        return List.of(
            RuntimeClientPlugin.builder()
                .pluginFunction(Symbol.builder()
                    .name("getEndpointPlugin")
                    .namespace(TypeScriptDependency.MIDDLEWARE_ENDPOINTS_V2.getPackageName(), "/")
                    .build())
                .additionalPluginFunctionParamsSymbolSupplier((m, s, o, sp) -> Map.of(
                    "endpointParameterInstructions", Symbol.builder()
                        .name(sp.toSymbol(o).getName() + ".getEndpointParameterInstructions()")
                        .build()
                ))
                .operationPredicate((m, s, o) -> s.hasTrait(EndpointRuleSetTrait.ID))
                .build(),
            RuntimeClientPlugin.builder()
                .resolveFunction(Symbol.builder()
                    .name("resolveClientEndpointParameters")
                    .namespace(EndpointsV2Generator.ENDPOINT_PARAMETERS_DEPENDENCY.getPackageName(), "/")
                    .build())
                .inputConfig(Symbol.builder()
                    .name("ClientInputEndpointParameters & EndpointInputConfig<EndpointParameters>")
                    .addReference(Symbol.builder()
                        .name("ClientInputEndpointParameters")
                        .namespace(EndpointsV2Generator.ENDPOINT_PARAMETERS_DEPENDENCY.getPackageName(), "/")
                        .build())
                    .addReference(Symbol.builder()
                        .name("EndpointInputConfig")
                        .namespace(TypeScriptDependency.MIDDLEWARE_ENDPOINTS_V2.getPackageName(), "/")
                        .addDependency(TypeScriptDependency.MIDDLEWARE_ENDPOINTS_V2)
                        .build())
                    .addReference(Symbol.builder()
                        .name("EndpointParameters")
                        .namespace(EndpointsV2Generator.ENDPOINT_PARAMETERS_DEPENDENCY.getPackageName(), "/")
                        .build())
                    .build())
                .resolvedConfig(Symbol.builder()
                    .name("ClientResolvedEndpointParameters & EndpointResolvedConfig<EndpointParameters>")
                    .addReference(Symbol.builder()
                        .name("ClientResolvedEndpointParameters")
                        .namespace(EndpointsV2Generator.ENDPOINT_PARAMETERS_DEPENDENCY.getPackageName(), "/")
                        .build())
                    .addReference(Symbol.builder()
                        .name("EndpointResolvedConfig")
                        .namespace(TypeScriptDependency.MIDDLEWARE_ENDPOINTS_V2.getPackageName(), "/")
                        .addDependency(TypeScriptDependency.MIDDLEWARE_ENDPOINTS_V2)
                        .build())
                    .addReference(Symbol.builder()
                        .name("EndpointParameters")
                        .namespace(EndpointsV2Generator.ENDPOINT_PARAMETERS_DEPENDENCY.getPackageName(), "/")
                        .build())
                    .build())
                .servicePredicate((m, s) -> s.hasTrait(EndpointRuleSetTrait.ID))
                .build()
            );
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, TypeScriptWriter>> interceptors(
        TypeScriptCodegenContext codegenContext
    ) {
        return List.of(
            CodeInterceptor.appender(SmithyContextCodeSection.class, (w, s) -> {
                if (!s.getService().hasTrait(EndpointRuleSetTrait.ID)) {
                    return;
                }
                w.openBlock("endpointRuleSet: {", "},", () -> {
                    w.write("getEndpointParameterInstructions: $T.getEndpointParameterInstructions,",
                        codegenContext.symbolProvider().toSymbol(s.getOperation()));
                });
            }),
            CodeInterceptor.appender(CommandPropertiesCodeSection.class, (w, s) -> {
                if (!s.getService().hasTrait(EndpointRuleSetTrait.ID)) {
                    return;
                }
                w.addImport("EndpointParameterInstructions", null, TypeScriptDependency.MIDDLEWARE_ENDPOINTS_V2);
                w.openBlock("""
                    public static getEndpointParameterInstructions(): EndpointParameterInstructions {""", "}", () -> {
                    w.openBlock("return {", "};", () -> {
                        ServiceShape service = s.getService();
                        OperationShape operation = s.getOperation();
                        Model model = codegenContext.model();
                        RuleSetParameterFinder parameterFinder = new RuleSetParameterFinder(service);
                        Shape operationInput = model.getShape(operation.getInputShape()).get();
                        Set<String> paramNames = new HashSet<>();

                        writeEndpointParameters(
                            parameterFinder.getStaticContextParamValues(operation),
                            (name, value) -> w.write("$L: { type: \"staticContextParams\", value: $L },", name, value),
                            paramNames);
                        writeEndpointParameters(
                            parameterFinder.getContextParams(operationInput),
                            (name, value) -> w.write("$L: { type: \"contextParams\", name: \"$L\" },", name, name),
                            paramNames);
                        writeEndpointParameters(
                            parameterFinder.getClientContextParams(),
                            (name, value) -> w.write("$L: { type: \"clientContextParams\", name: \"$L\" },",
                                name, EndpointsParamNameMap.getLocalName(name)),
                            paramNames);
                        writeEndpointParameters(
                            parameterFinder.getBuiltInParams(),
                            (name, value) -> w.write("$L: { type: \"builtInParams\", name: \"$L\" },",
                                name, EndpointsParamNameMap.getLocalName(name)),
                            paramNames);
                    });
                });
            }),
            CodeInterceptor.appender(IndexClientExportsCodeSection.class, (w, s) -> {
                if (s.getService().hasTrait(EndpointRuleSetTrait.class)) {
                    w.write("export { ClientInputEndpointParameters } from \"./endpoint/EndpointParameters\";");
                }
            })
        );
    }

    private void writeEndpointParameters(
        Map<String, String> endpointParameters,
        BiConsumer<String, String> entryWriter,
        Set<String> writtenParameterNames
    ) {
        endpointParameters.forEach((name, value) -> {
            if (!writtenParameterNames.contains(name)) {
                entryWriter.accept(name, value);
                writtenParameterNames.add(name);
            }
        });
    }
}
