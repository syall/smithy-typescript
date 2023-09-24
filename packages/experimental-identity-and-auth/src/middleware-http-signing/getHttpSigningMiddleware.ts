import { retryMiddlewareOptions } from "@smithy/middleware-retry";
import { FinalizeRequestHandlerOptions, Pluggable, RelativeMiddlewareOptions } from "@smithy/types";

import { httpSigningMiddleware } from "./httpSigningMiddleware";

/**
 * @internal
 */
export const httpSigningMiddlewareOptions: FinalizeRequestHandlerOptions & RelativeMiddlewareOptions = {
  step: "finalizeRequest",
  tags: ["HTTP_SIGNING"],
  name: "httpSigningMiddleware",
  override: true,
  relation: "after",
  toMiddleware: retryMiddlewareOptions.name!,
};

/**
 * @internal
 */
export const getHttpSigningPlugin = <T>(
  config: T
): Pluggable<any, any> => ({
  applyToStack: (clientStack) => {
    clientStack.addRelativeTo(httpSigningMiddleware(config), httpSigningMiddlewareOptions);
  },
});
