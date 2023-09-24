import { endpointMiddlewareOptions } from "@smithy/middleware-endpoint";
import { MetadataBearer, Pluggable, RelativeMiddlewareOptions, SerializeHandlerOptions } from "@smithy/types";

import { httpAuthSchemeMiddleware, PreviouslyResolved } from "./httpAuthSchemeMiddleware";

/**
 * @internal
 */
export const httpAuthSchemeMiddlewareOptions: SerializeHandlerOptions & RelativeMiddlewareOptions = {
  step: "serialize",
  tags: ["HTTP_AUTH_SCHEME"],
  name: "httpAuthSchemeMiddleware",
  override: true,
  relation: "before",
  toMiddleware: endpointMiddlewareOptions.name!,
};

/**
 * @internal
 */
export const getHttpAuthSchemePlugin = <T>(
  config: T & PreviouslyResolved
): Pluggable<any, any> => ({
  applyToStack: (clientStack) => {
    clientStack.addRelativeTo(httpAuthSchemeMiddleware(config), httpAuthSchemeMiddlewareOptions);
  },
});
