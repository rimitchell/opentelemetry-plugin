package io.jenkins.plugins.opentelemetry;

import io.jenkins.plugins.opentelemetry.semconv.JenkinsOtelSemanticAttributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OpenTelemetryTest {

    private Logger logger = Logger.getLogger(OpenTelemetryTest.class.getName());

    @Test
    public void test() throws Exception {
        JenkinsOtelPlugin jenkinsOtelPlugin = new JenkinsOtelPlugin();


        Tracer tracer = jenkinsOtelPlugin.getTracer();
        Meter meter = jenkinsOtelPlugin.getMeter();
        OpenTelemetrySdk openTelemetry = jenkinsOtelPlugin.getOpenTelemetrySdk();

        LongCounter myMetric = meter.longCounterBuilder("my-metric").build();
        myMetric.add(1);
        System.out.println("myMetric");

        SpanBuilder rootSpanBuilder = tracer.spanBuilder("ci.pipeline.run")
                .setAttribute(JenkinsOtelSemanticAttributes.CI_PIPELINE_ID, "my-pipeline")
                .setAttribute(JenkinsOtelSemanticAttributes.CI_PIPELINE_NAME, "my pipeline")
                .setAttribute(JenkinsOtelSemanticAttributes.CI_PIPELINE_RUN_NUMBER, 12l);

        Span rootSpan = rootSpanBuilder.startSpan();
        System.out.println("Root span object: " + rootSpan.getClass() + ", " + rootSpan);
        SpanData rootSpanData = ((ReadableSpan) rootSpan).toSpanData();

        try (Scope scope = rootSpan.makeCurrent()) {
            Thread.sleep(1_000);

            System.out.println("OPEN TELEMETRY FORCE FLUSH");
            CompletableResultCode completableResultCode = openTelemetry.getTracerManagement().forceFlush();

            completableResultCode.join(5, TimeUnit.SECONDS);
        } finally {
            rootSpan.end();
        }


        openTelemetry.getTracerManagement().shutdown();
    }

    @After
    public void after() {

    }
}