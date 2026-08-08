/**
 * Request admission control: the token bucket rate limiter backed by a Redis Lua script,
 * and the budget guard that rejects requests once a key's spend ceiling is reached.
 */
package com.adisrivastava.gateway.ratelimit;
