/**
 * Application boundary: use-case services and the ports (interfaces) the core depends on,
 * such as {@code EventRecorder} and {@code RunRepository}.
 *
 * <p>Infrastructure supplies the implementations (Dependency Inversion).
 * Ports are introduced when first needed (persistence ports in M9).
 */
package com.afterfall.application;
