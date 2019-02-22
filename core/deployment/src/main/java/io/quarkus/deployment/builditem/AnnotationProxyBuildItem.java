package io.quarkus.deployment.builditem;

import java.lang.annotation.Annotation;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.jandex.AnnotationInstance;

import io.quarkus.deployment.recording.AnnotationProxyProvider;
import io.quarkus.deployment.recording.AnnotationProxyProvider.AnnotationProxyBuilder;
import io.quarkus.runtime.annotations.Template;

/**
 * Create annotation proxies that can be used as {@link Template} parameters.
 */
public final class AnnotationProxyBuildItem extends SimpleBuildItem {

    private final AnnotationProxyProvider provider;

    public AnnotationProxyBuildItem(AnnotationProxyProvider provider) {
        this.provider = provider;
    }

    /**
     * 
     * @param annotationInstance
     * @param annotationType
     * @return a new annotation proxy builder
     */
    public <A extends Annotation> AnnotationProxyBuilder<A> builder(AnnotationInstance annotationInstance,
            Class<A> annotationType) {
        return provider.builder(annotationInstance, annotationType);
    }

    /**
     * 
     * @param annotationInstance
     * @param annotationType
     * @return a new annotation proxy
     */
    public <A extends Annotation> A from(AnnotationInstance annotationInstance, Class<A> annotationType) {
        return provider.builder(annotationInstance, annotationType).build();
    }

}
