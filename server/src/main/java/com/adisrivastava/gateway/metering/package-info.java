/**
 * Usage metering pipeline: publish to a Redis Stream off the request path, consume in
 * batches, bulk insert, and roll up hourly aggregates.
 */
package com.adisrivastava.gateway.metering;
