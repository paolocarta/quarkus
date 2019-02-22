/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;
import io.quarkus.creator.phase.augment.AugmentPhase;
import io.quarkus.creator.phase.curate.CurateOutcome;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;
import io.quarkus.creator.phase.runnerjar.RunnerJarPhase;
import io.quarkus.creator.resolver.maven.ResolvedMavenArtifactDeps;

/**
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildMojo extends AbstractMojo {

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
    private List<RemoteRepository> pluginRepos;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The directory for application classes transformed by processing.
     */
    @Parameter(defaultValue = "${project.build.directory}/transformed-classes")
    private File transformedClassesDirectory;

    /**
     * The directory for classes generated by processing.
     */
    @Parameter(defaultValue = "${project.build.directory}/wiring-classes")
    private File wiringClassesDirectory;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;
    /**
     * The directory for library jars
     */
    @Parameter(defaultValue = "${project.build.directory}/lib")
    private File libDir;

    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(defaultValue = "io.quarkus.runner.GeneratedMain")
    private String mainClass;

    @Parameter(defaultValue = "true")
    private boolean useStaticInit;

    @Parameter(defaultValue = "false")
    private boolean uberJar;

    public BuildMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException {
        try (AppCreator appCreator = AppCreator.builder()
                // configure the build phases we want the app to go through
                .addPhase(new AugmentPhase()
                        .setAppClassesDir(outputDirectory.toPath())
                        .setTransformedClassesDir(transformedClassesDirectory.toPath())
                        .setWiringClassesDir(wiringClassesDirectory.toPath()))
                .addPhase(new RunnerJarPhase()
                        .setLibDir(libDir.toPath())
                        .setFinalName(finalName)
                        .setMainClass(mainClass)
                        .setUberJar(uberJar))
                .setWorkDir(buildDir.toPath())
                .build()) {

            final AppArtifact appArtifact = new AppArtifact(project.getGroupId(), project.getArtifactId(),
                    project.getVersion());
            final List<AppDependency> appDeps = new ResolvedMavenArtifactDeps(project.getGroupId(), project.getArtifactId(),
                    project.getVersion(), project.getArtifacts()).collectDependencies(appArtifact);

            // push resolved application state
            appCreator.pushOutcome(CurateOutcome.builder()
                    .setAppArtifact(appArtifact)
                    .setInitialDeps(appDeps)
                    .build());

            // resolve the outcome we need here
            appCreator.resolveOutcome(RunnerJarOutcome.class);
        } catch (AppCreatorException e) {
            throw new MojoExecutionException("Failed to build a runnable JAR", e);
        }
    }
}
