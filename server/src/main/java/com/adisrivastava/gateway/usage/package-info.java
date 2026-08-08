/**
 * Usage and analytics read API. Queries the hourly rollup tables only, never the raw
 * request log, so dashboard reads stay cheap.
 */
package com.adisrivastava.gateway.usage;
