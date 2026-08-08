/**
 * Semantic cache: embedding generation, pgvector similarity lookup, and the single-flight
 * lock that collapses concurrent misses for the same prompt.
 */
package com.adisrivastava.gateway.cache;
