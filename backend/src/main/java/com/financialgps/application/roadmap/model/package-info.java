/**
 * Transport-shaped read models of the roadmap ({@code RoadmapModels}).
 *
 * <p>Amounts and ratios travel as decimal strings, dates as ISO-8601 and {@code currency} is
 * echoed once at route level (the 001/002/003 {@code Money} contract). A value that is not
 * applicable or not calculable is {@code null} and always carries a reason code, a blocker or a
 * route explanation — never an available {@code "0.00"}.
 */
package com.financialgps.application.roadmap.model;
