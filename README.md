# What is it

The `maven-dependency-versions-check-plugin` is a Maven plugin that verifies that the resolved versions of
dependencies are mutually compatible with each other. While Maven does a good job in dependency resolution,
it usually applied the "higher version wins" algorithm to select a dependency and is not aware of any semantic
notions of version number (e.g. that artifacts with different major version numbers are not comaptible. 

More specifically, it will check that

* The resolved version of every dependency declared explicitly in the current POM is the same or at least
  compatible with the one that was stated.
* For every explicitly declared dependency in the current POM, all its dependency versions are met. I.e. the
  resolved versions for all dependencies are compatible to the version stated in that dependency's POM.

The plugin can configured to issue warnings or fail the build in that case.

# How to get it

The plugin is available in Maven Central, so you can get it by simply adding it to your `pom.xml` file:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>maven-dependency-versions-check-plugin</artifactId>
  <version>2.0.2</version>
</plugin>
```

To make commandline usage a bit easier, you should add the `com.ning.maven.plugins` group to the
`pluginGroups` section in your `settings.xml` file:

```xml
<settings>
  ...
  <pluginGroups>
    <pluginGroup>com.ning.maven.plugins</pluginGroup>
  </pluginGroups>
  ...
</settings>
```

# How to use it

The plugin as two goals:

* `com.ning.maven.plugins:maven-dependency-versions-check-plugin:list` lists out all dependencies in the project
* `com.ning.maven.plugins:maven-dependency-versions-check-plugin:check` checks all the dependencies in the project
  and can fail the build.

## The "list" goal

This goal reports a list of all dependencies that are used in the current project:

```
% mvn com.ning.maven.plugins:maven-dependency-versions-check-plugin:list

[...]
[INFO] [dependency-versions-check:list {execution: default-cli}]
[INFO] Transitive dependencies for scope 'compile':
[INFO] backport-util-concurrent:backport-util-concurrent: backport-util-concurrent:backport-util-concurrent-3.1 (3.1)
[INFO] classworlds:classworlds:                           classworlds:classworlds-1.1-alpha-2 (1.1-alpha-2)
[INFO] com.pyx4j:maven-plugin-log4j:                      com.pyx4j:maven-plugin-log4j-1.0.1 (*1.0.1*)
[INFO] commons-collections:commons-collections:           commons-collections:commons-collections-3.2.1 (*3.2.1*)
[INFO] commons-lang:commons-lang:                         commons-lang:commons-lang-2.5 (*2.5*)
[INFO] junit:junit:                                       junit:junit-3.8.1 (3.8.1)
[INFO] log4j:log4j:                                       log4j:log4j-1.2.16 (1.2.14, *1.2.16*)
[INFO] org.apache.maven.wagon:wagon-provider-api:         org.apache.maven.wagon:wagon-provider-api-1.0-beta-6 (1.0-beta-6)
[INFO] org.apache.maven:maven-artifact:                   org.apache.maven:maven-artifact-2.2.1 (2.0, 2.2.1)
[INFO] org.apache.maven:maven-artifact-manager:           org.apache.maven:maven-artifact-manager-2.2.1 (2.2.1)
[INFO] org.apache.maven:maven-model:                      org.apache.maven:maven-model-2.2.1 (2.2.1)
[INFO] org.apache.maven:maven-plugin-api:                 org.apache.maven:maven-plugin-api-2.2.1 (2.0, *2.2.1*)
[INFO] org.apache.maven:maven-plugin-registry:            org.apache.maven:maven-plugin-registry-2.2.1 (2.2.1)
[INFO] org.apache.maven:maven-profile:                    org.apache.maven:maven-profile-2.2.1 (2.2.1)
[INFO] org.apache.maven:maven-project:                    org.apache.maven:maven-project-2.2.1 (*2.2.1*)
[INFO] org.apache.maven:maven-repository-metadata:        org.apache.maven:maven-repository-metadata-2.2.1 (2.2.1)
[INFO] org.apache.maven:maven-settings:                   org.apache.maven:maven-settings-2.2.1 (2.2.1)
[INFO] org.codehaus.plexus:plexus-container-default:      org.codehaus.plexus:plexus-container-default-1.0-alpha-9-stable-1 (1.0-alpha-9-stable-1)
[INFO] org.codehaus.plexus:plexus-interpolation:          org.codehaus.plexus:plexus-interpolation-1.11 (1.11)
[INFO] org.codehaus.plexus:plexus-utils:                  org.codehaus.plexus:plexus-utils-1.5.15 (1.0.4, 1.5.15)
[INFO] org.slf4j:slf4j-api:                               org.slf4j:slf4j-api-1.6.1 (*1.6.1*)
[INFO] org.slf4j:slf4j-log4j12:                           org.slf4j:slf4j-log4j12-1.6.1 (*1.6.1*)
[...]
```

This is the dependency list for the plugin itself. Every line contains of the following elements:

* actual dependency - The dependency that was resolved based on the project POM.
* resolved dependency - The dependency that was chosen to be included.
* additional versions - In braces, one or more dependency versions. These are the versions that were under
  consideration when choosing the final dependency. 

If a dependency is surrounded by `*`, it was declared in the project POM.
If a dependency is surrounded by `!`, then it is in conflict. Running the check plugin will flag this as error
and might fail the build.

### Options for the `list` goal

These options are intended to be given on the command line. They can also be configured in the config section
(see below) but are less useful.

* `scope` - selects the scope for the dependency list. Can be `compile` (the default), `test` or `runtime`.
* `directOnly` (boolean) - if present, only list dependencies that are declared in the project POM. Transitive
 versions are still resolved and additional versions might be listed.
* `conflictsOnly` (boolean) - list only dependencies that are in conflict.

## The `check` goal

This goal checks all the dependencies, reports only problems and might fail the build if configured.

It is intended to be run as part of the normal build cycle. In this case, it should be added like this:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>maven-dependency-versions-check-plugin</artifactId>
  <version>2.0.2</version>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

If the plugin finds a conflict, then it will output something like

```
[INFO] Checking dependency versions
[WARNING] Found a problem with the direct dependency log4j:log4j of the current project
  Expected version is 1.2.13
  Resolved version is 1.2.13
  Version 1.2.16 was expected by artifact: org.jboss.netty:netty
```

The above sample shows a conflict with a dependency declared in the current POM. In this case, it will print

* the expected version of the current POM
* the resolved version (i.e. effective POM)
* all versions that were encountered during dependency resolution plus the dependencies that brought them in.

If the problem is instead between dependencies, then it will omit the expected version.

The above will change to `[ERROR]` if the plugin is configured to fail the build in case of conflicts (see below).

# POM configuration section

Note also that any configuration in a POM overrides the default configuration (e.g. from the parent POM), so you
should duplicate that configuration or use the very arcane `combine.children` trick (see
http://www.sonatype.com/people/2007/06/how-to-merge-sub-items-from-parent-pom-to-child-pom-in-a-maven-plugin-configuration-2/
for details...).

## `exceptions`

In rare cases you need to configure the plugin to make exceptions, i.e. allow specific version conflicts. For
instance, in the above example you might insist that `log4j` version 1.2.13 is fine:  

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>maven-dependency-versions-check-plugin</artifactId>
  <version>2.0.2</version>
  <configuration>
    <exceptions>
      <exception>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <expectedVersion>1.2.13</expectedVersion>
        <resolvedVersion>1.2.16</resolvedVersion>
      </exception>
    </exceptions>
  </configuration>
</plugin>
```

An exception is only for a specific version conflict, in this case `1.2.13` instead of `1.2.16`. If the plugin
finds that a different version of `log4j` was resolved or required, say `1.2.15`, then you will get additional
warnings/errors that would have to be handled with additional exceptions.

An exception must have all four elements (`groupId`, `artifactId`, `expectedVersion` and `resolvedVersion`)
present.

## `warnIfMajorVersionIsHigher`

Boolean flag that enables warnings if a dependency that was excluded from version resolution depends on an
incompatible version (for historical reasons it is called `warnIfMajorVersionIsHigher`). Default value is
`false` (do not warn).

```xml
<configuration>
  <warnIfMajorVersionIsHigher>true</warnIfMajorVersionIsHigher>
</configuration>
```

## `failBuildInCaseOfConflict` (`check` only)

Boolean flag that will turn warnings about dependency problems into error messages and fail the build accordingly.
Default value is `false` (only report warnings).

```xml
<configuration>
  <failBuildInCaseOfConflict>true</failBuildInCaseOfConflict>
</configuration>
```

## `resolvers`

Defines a version strategy resolver. Version strategy resolvers are used to determine which strategy to apply
to decide whether two versions are compatible with each other.

```xml
<configuration>
  <resolvers>
    <resolver>
      <id>apache-dependencies</id>
      <strategyName>apr</strategyName>
      <includes>
        <include>commons-configuration:commons-configuration</include>
        <include>org.apache.commons:*</include>
        <include>org.apache.maven.plugins.*</include>
        <include>org.apache*</include>
      </includes>
    </resolver>
  </resolvers>
</configuration>
```

A resolver contains a strategy name (`<strategyName>`) and a list of one or more includes to list the patterns
of artifacts for which this resolvers should be used. Patterns can contain wildcards (*) for both group and
artifactName.

See below for more details on strategy resolvers.

## `defaultStrategy`

Selects the default strategy to fall back on when no match is found in the resolvers configuration. Defaults to
`default`.

See below for more details on strategies.

# Version resolving strategies

Maven uses a "first version wins" approach to resolving dependencies (see below for more details) which
is not always the best way to go. Often, version numbers also carry semantic meaning (e.g. Apache APR and SemVer
versionings assign indicators for forward- and backward-compatibility on each of the elements in a three-digit
version number). This plugin provides an indicator whether a set of dependencies resolved by maven actually
"fits" and may fail at build time, thus avoiding failing at run time.

To do so, the plugin employs version resolving strategies. In the most simple use case, it uses the default
strategy exclusively. But by using the `<resolvers>` section, it is possible to tweak this and to allow custom
strategies to be used.

## Strategies included with the plugin

#### `default` - the default strategy

This strategy is modelled after the actual maven version resolution.

It assumes that all smaller versions are compatible when replaced with larger numbers and compares version
elements from left to right. E.g. `3.2.1` > `3.2` and `2.1.1` > `1.0`. It usually works pretty ok and is the
fallback for version resolution (unless changed with the `<defaultStrategy>` setting in the configuration section.

#### `apr` - Apache APR versioning

Three digit versioning first [defined by the APR project](https://apr.apache.org/versioning.html). It assumes that
for two versions to be compatible, the first digit must be identical, the middle digit indicates backwards
compatibility (i.e. `1.2.x` can replace `1.1.x` but `1.4.x` can not replace `1.5.x`) and the third digit
signifies the patch level (only bug fixes, full API compatibility).
This is similar to the [SemVer versioning scheme](http://semver.org/).

#### `two-digits-backward-compatible` - Relaxed APR versioning

Similar to APR, but assumes that there is no "major" version digit (e.g. it is part of the artifact id). All
versions are backwards compatible. First digit must be the same or higher to be compatible (i.e. `2.0` can
replace `1.2`).

#### `single-digit` - Single version number

The version consists of a single number. Larger versions can replace smaller versions. The version number may
contain additional letters or prefixes (i.e. `r08` can replace `r07`).

## Writing your own strategies (advanced usage)

A custom strategy must implement the `com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy`
interface and must declare itself as a plexus component. A jar containing a custom strategy can then used as a
custom dependency of the plugin:

```java
/**
 * @plexus.component role="com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy"
 *                   role-hint="bad"
 */
public class BadStrategy implements Strategy
{
    public String getName() { return "bad"; }

    public boolean isCompatible(Version a, Version b) { return false; };
}
```

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>maven-dependency-versions-check-plugin</artifactId>
  <version>2.0.2</version>
  <dependencies>
    <dependency>
      <groupId>badExample</groupId>
      <artifactId>badStrategy</artifactId>
      <version>1.0</version>
    </dependency>
  </dependencies>
.....
</plugin>
```

See the source code to the plugin and the existing strategies for examples on how to write strategies.

# How to resolve conflicts

Some more detailed explanation is below in the background section.

In general, you should try to upgrade dependency versions if you can make sure that they work (e.g. via unit or 
other tests).
If you cannot do that, then either add exclusions or add an explicit dependency in the current POM.
If even this fails, then add an exception configuration, but please use this only as a last resort.
In this case you should add comments to the exceptions, exclusions or explicit dependencies that state why you
added them (e.g. noting the version conflict).

## Background: Maven 2's arbitrary version resolution strategy

Maven resolves conflicting versions with a simple strategy with sometimes surprising results. Somewhat
simplified, the rules to resolve which version to choose when encountering a given dependency multiple times,
are as follows:

* If there is an enforced version/version range (use of `[]` or `()` in the version string), then Maven will
  prefer that, even if in a transitive dependency. (Forced versions are evil, especially for libraries. Don't
  use them.)
* If the `pom.xml` of the project contains the dependency, Maven will use that version.
* Otherwise it will pick the first (transitive) dependency it finds in a depth-first search.

Maven's default version resolution strategy is best explained with examples. All of the examples presented
in the following can be found in the `examples` directory where you execute them yourself if you'd like to.
Each of the examples consists of a multi-module project that includes a project `a` which generates an
executable uber-jar that is executed as part of the build. The main class of the project will try to load
classes introduced in specific [guava](https://code.google.com/p/guava-libraries/) versions and depending on
the `a`'s dependencies some of these may not be available.

Unfortunately by default Maven doesn't tell much you about the versions it encounters. You can use the
[Maven dependency plugin's tree goal](http://maven.apache.org/plugins/maven-dependency-plugin/tree-mojo.html)
with the `-Dverbose` option to gain more insight, but that plugin offers little in the way of enforcing sane
versioning. This is where the `maven-dependency-versions-check-plugin` plugin comes in.

#### [Example 1](examples/example1)

The first example forms the base for the other examples. It consist of a simple hierarchy of three projects,
`a`, `b`, and `c`, where `a` depends on both `b` and `c`, in that order. All three depend on `guava` with these
versions:

* `a` depends on version `16.0`
* `b` depends on version `14.0`
* `c` depends on version `15.0`

Since `a` has a direct dependency on `guava`, we expect Maven to use that. Run Maven on the parent `pom.xml` of
the three projects and you should see this for `a`:

```
[INFO] --- maven-dependency-plugin:2.8:tree (dependency-tree) @ a ---
[INFO] example1:a:jar:1.0.0-SNAPSHOT
[INFO] +- example1:b:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:14.0:compile - omitted for conflict with 16.0)
[INFO] +- example1:c:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:15.0:compile - omitted for conflict with 14.0)
[INFO] \- com.google.guava:guava:jar:16.0:compile
[INFO] 
[INFO] --- maven-dependency-versions-check-plugin:2.0.2:check (dependency-versions-check) @ b ---
[INFO] Checking dependency versions
[INFO] 

...

[INFO] --- exec-maven-plugin:1.2.1:java (default) @ a ---
Loading class added in Guava 14
Success: com.google.common.collect.ImmutableRangeMap
Loading class added in Guava 15
Success: com.google.common.base.StandardSystemProperty
Loading class added in Guava 16
Success: com.google.common.base.Converter
```

which means that yes, we get version `16.0`.

#### [Example 2](examples/example2)

The second example has the same structure as example 1, but the versions are different:

* `a` depends on version `15.0`
* `b` depends on version `16.0`
* `c` depends on version `14.0`

Ideally we would like Maven to select version `16.0` since `b` depends on it, but unfortunately because of `a`
having a direct dependency on `guava`, we'll get `15.0` instead:

```
[INFO] --- maven-dependency-plugin:2.8:tree (dependency-tree) @ a ---
[INFO] example2:a:jar:1.0.0-SNAPSHOT
[INFO] +- example2:b:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:16.0:compile - omitted for conflict with 15.0)
[INFO] +- example2:c:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:14.0:compile - omitted for conflict with 16.0)
[INFO] \- com.google.guava:guava:jar:15.0:compile
[INFO] 
[INFO] --- maven-dependency-versions-check-plugin:2.0.2:check (dependency-versions-check) @ a ---
[INFO] Checking dependency versions
[WARNING] Found a problem with the direct dependency com.google.guava:guava of the current project
  Expected version is 15.0
  Resolved version is 15.0
  Version 14.0 was expected by artifact: example2:c
  Version 16.0 was expected by artifact: example2:b
[INFO] 

...

[INFO] --- exec-maven-plugin:1.2.1:java (default) @ a ---
Loading class added in Guava 14
Success: com.google.common.collect.ImmutableRangeMap
Loading class added in Guava 15
Success: com.google.common.base.StandardSystemProperty
Loading class added in Guava 16
[WARNING] 
java.lang.reflect.InvocationTargetException
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:606)
  at org.codehaus.mojo.exec.ExecJavaMojo$1.run(ExecJavaMojo.java:297)
  at java.lang.Thread.run(Thread.java:744)
Caused by: java.lang.ClassNotFoundException: com.google.common.base.Converter
  at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
  at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
  at java.security.AccessController.doPrivileged(Native Method)
  at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
  at java.lang.Class.forName0(Native Method)
  at java.lang.Class.forName(Class.java:190)
  at examples.example2.a.Main.main(Main.java:14)
  ... 6 more
[
```

#### [Example 3](examples/example3)

In the third example, `a` doesn't have a direct dependency on `guava`. `b` and `c` depend on these versions:

* `b` depends on version `15.0`
* `c` depends on version `16.0`

Since `a`'s dependency on `b` comes before `c`, Maven will choose `b`'s dependency on `guava` (`15.0`):

```
[INFO] --- maven-dependency-plugin:2.8:tree (dependency-tree) @ a ---
[INFO] example3:a:jar:1.0.0-SNAPSHOT
[INFO] +- example3:b:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- com.google.guava:guava:jar:15.0:compile
[INFO] \- example3:c:jar:1.0.0-SNAPSHOT:compile
[INFO]    \- (com.google.guava:guava:jar:16.0:compile - omitted for conflict with 15.0)
[INFO] 
[INFO] --- maven-dependency-versions-check-plugin:2.0.2:check (dependency-versions-check) @ a ---
[INFO] Checking dependency versions
[WARNING] Found a problem with the dependency com.google.guava:guava
  Resolved version is 15.0
  Version 15.0 was expected by artifact: example3:b
  Version 16.0 was expected by artifact: example3:c
[INFO] 

...

[INFO] --- exec-maven-plugin:1.2.1:java (default) @ a ---
Loading class added in Guava 14
Success: com.google.common.collect.ImmutableRangeMap
Loading class added in Guava 15
Success: com.google.common.base.StandardSystemProperty
Loading class added in Guava 16
[WARNING] 
java.lang.reflect.InvocationTargetException
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:606)
  at org.codehaus.mojo.exec.ExecJavaMojo$1.run(ExecJavaMojo.java:297)
  at java.lang.Thread.run(Thread.java:744)
Caused by: java.lang.ClassNotFoundException: com.google.common.base.Converter
  at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
  at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
  at java.security.AccessController.doPrivileged(Native Method)
  at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
  at java.lang.Class.forName0(Native Method)
  at java.lang.Class.forName(Class.java:190)
  at examples.example3.a.Main.main(Main.java:14)
  ... 6 more
```

#### [Example 4](examples/example4)

This is the same as the third example, except that this time `a` depends on `c` before `b`. Thus we now get
`16.0` as the resolved `guava` version:

```
[INFO] --- maven-dependency-plugin:2.8:tree (dependency-tree) @ a ---
[INFO] example4:a:jar:1.0.0-SNAPSHOT
[INFO] +- example4:c:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- com.google.guava:guava:jar:16.0:compile
[INFO] \- example4:b:jar:1.0.0-SNAPSHOT:compile
[INFO]    \- (com.google.guava:guava:jar:15.0:compile - omitted for conflict with 16.0)
[INFO] 
[INFO] --- maven-dependency-versions-check-plugin:2.0.2:check (dependency-versions-check) @ a ---
[INFO] Checking dependency versions
[INFO] 

...

[INFO] --- exec-maven-plugin:1.2.1:java (default) @ a ---
Loading class added in Guava 14
Success: com.google.common.collect.ImmutableRangeMap
Loading class added in Guava 15
Success: com.google.common.base.StandardSystemProperty
Loading class added in Guava 16
Success: com.google.common.base.Converter
```

#### [Example 5](examples/example5)

This is another variation of the third example, but this time guava is pulled as a transitive dependency
via `com.fasterxml.jackson.datatype:jackson-datatype-guava`:

* `b` depends on `jackson-datatype-guava` version `2.3.0` which depends on `guava` version `14.0.1`
* `c` depends on `jackson-datatype-guava` version `2.3.1` which depends on `guava` version `15.0`

```
[INFO] --- maven-dependency-plugin:2.8:tree (dependency-tree) @ a ---
[INFO] example5:a:jar:1.0.0-SNAPSHOT
[INFO] +- example5:b:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- com.fasterxml.jackson.datatype:jackson-datatype-guava:jar:2.3.0:compile
[INFO] |     +- com.fasterxml.jackson.core:jackson-databind:jar:2.3.0:compile
[INFO] |     |  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.3.0:compile
[INFO] |     |  \- (com.fasterxml.jackson.core:jackson-core:jar:2.3.0:compile - omitted for duplicate)
[INFO] |     +- com.fasterxml.jackson.core:jackson-core:jar:2.3.0:compile
[INFO] |     \- com.google.guava:guava:jar:14.0.1:compile
[INFO] \- example5:c:jar:1.0.0-SNAPSHOT:compile
[INFO]    \- (com.fasterxml.jackson.datatype:jackson-datatype-guava:jar:2.3.1:compile - omitted for conflict with 2.3.0)
[INFO] 
[INFO] --- maven-dependency-versions-check-plugin:2.0.2:check (dependency-versions-check) @ a ---
[INFO] Checking dependency versions
[WARNING] Found a problem with the dependency com.fasterxml.jackson.core:jackson-core
  Resolved version is 2.3.0
  Version 2.3.0 was expected by artifact: example5:b
  Version 2.3.1 was expected by artifact: example5:c
[WARNING] Found a problem with the dependency com.fasterxml.jackson.core:jackson-databind
  Resolved version is 2.3.0
  Version 2.3.0 was expected by artifact: example5:b
  Version 2.3.1 was expected by artifact: example5:c
[WARNING] Found a problem with the dependency com.fasterxml.jackson.datatype:jackson-datatype-guava
  Resolved version is 2.3.0
  Version 2.3.0 was expected by artifact: example5:b
  Version 2.3.1 was expected by artifact: example5:c
[WARNING] Found a problem with the dependency com.google.guava:guava
  Resolved version is 14.0.1
  Version 14.0.1 was expected by artifact: example5:b
  Version 15.0 was expected by artifact: example5:c
[INFO] 

...


[INFO] --- exec-maven-plugin:1.2.1:java (default) @ a ---
Loading class added in Guava 14
Success: com.google.common.collect.ImmutableRangeMap
Loading class added in Guava 15
[WARNING] 
java.lang.reflect.InvocationTargetException
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:606)
  at org.codehaus.mojo.exec.ExecJavaMojo$1.run(ExecJavaMojo.java:297)
  at java.lang.Thread.run(Thread.java:744)
Caused by: java.lang.ClassNotFoundException: com.google.common.base.StandardSystemProperty
  at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
  at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
  at java.security.AccessController.doPrivileged(Native Method)
  at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
  at java.lang.Class.forName0(Native Method)
  at java.lang.Class.forName(Class.java:190)
  at examples.example5.a.Main.main(Main.java:10)
  ... 6 more
```

Note how the `maven-dependency-plugin` doesn't mention a conflict with `guava`. This is because `c`'s
dependency on `jackson-datatype-guava` is omitted completely and thus all its transitive dependencies.
The `maven-dependency-versions-check-plugin` however will tell you that you get the wrong guava version.

#### [Example 6](examples/example6)

The sixth example is a variation of example 1 where `c` enforces version `15.0` of guava:

* `a` depends on version `16.0`
* `b` depends on version `14.0`
* `c` depends on version `[15.0]`

You'd expect this to fail, but Maven happily resolves it:

```
[INFO] --- maven-dependency-plugin:2.8:tree (dependency-tree) @ a ---
[INFO] example6:a:jar:1.0.0-SNAPSHOT
[INFO] +- example6:b:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:15.0:compile - omitted for duplicate)
[INFO] +- example6:c:jar:1.0.0-SNAPSHOT:compile
[INFO] |  \- (com.google.guava:guava:jar:15.0:compile - omitted for duplicate)
[INFO] \- com.google.guava:guava:jar:15.0:compile
[INFO] 
[INFO] --- maven-dependency-versions-check-plugin:2.0.2:check (dependency-versions-check) @ a ---
[INFO] Checking dependency versions
[WARNING] Found a problem with the direct dependency com.google.guava:guava of the current project
  Expected version is 16.0
  Resolved version is 15.0
  Version 14.0 was expected by artifact: example6:b
  Version 15.0 was expected by artifact: example6:c
[INFO] 

...

[INFO] --- exec-maven-plugin:1.2.1:java (default) @ a ---
Loading class added in Guava 14
Success: com.google.common.collect.ImmutableRangeMap
Loading class added in Guava 15
Success: com.google.common.base.StandardSystemProperty
Loading class added in Guava 16
[WARNING] 
java.lang.reflect.InvocationTargetException
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:606)
  at org.codehaus.mojo.exec.ExecJavaMojo$1.run(ExecJavaMojo.java:297)
  at java.lang.Thread.run(Thread.java:744)
Caused by: java.lang.ClassNotFoundException: com.google.common.base.Converter
  at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
  at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
  at java.security.AccessController.doPrivileged(Native Method)
  at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
  at java.lang.Class.forName0(Native Method)
  at java.lang.Class.forName(Class.java:190)
  at examples.example6.a.Main.main(Main.java:14)
  ... 6 more
```

Note also how the `maven-dependency-plugin` is lying to us about `b`'s dependency version, telling us it
depends on `15.0` whereas it actually depends on version `14.0`.

## Strategies to resolve conflicts

The common strategies to handle dependency version conflicts are:

### Specify an explicit dependency

```xml
<project>
  <groupId>...</groupId>
  <artifactId>a</artifactId>
  ...
  <dependencies>
    <dependency>
      <groupId>...</groupId>
      <artifactId>b</artifactId>
      <version>...</version>
    </dependency>
    <dependency>
      <groupId>...</groupId>
      <artifactId>c</artifactId>
      <version>...</version>
    </dependency>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>16.0</version>
    </dependency>
  </dependencies>
</project>
```

In this case, Maven uses the version that is specified in `a`'s POM and ignores both `b` and `c`'s dependencies.

### Use dependency management in the parent POM

If `b` and `c` use a common parent POM, then the version of `guava` can be specified in that parent POM, thus
ensuring that they use the same version:

```xml
<project>
  <groupId>...</groupId>
  <artifactId>parentOfBAndC</artifactId>
  ...
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>16.0</version>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

This does not declare a dependency of `parentOfBAndC` to `guava`, but instead defines the version for all POMs
that have this POM as a parent (unless a version is stated in the child POM). `b` and `c` then can be changed to
this:

```xml
<project>
  <parent>
    <groupId>...</groupId>
    <artifactId>parentOfBAndC</artifactId>
  </parent>
  <groupId>...</groupId>
  <artifactId>b</artifactId>
  ...
  <dependencies>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
    </dependency>
  </dependencies>
</project>
```

```xml
<project>
  <parent>
    <groupId>...</groupId>
    <artifactId>parentOfBAndC</artifactId>
  </parent>
  <groupId>...</groupId>
  <artifactId>c</artifactId>
  ...
  <dependencies>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
    </dependency>
  </dependencies>
</project>
```

This way, `b` and `c` will get the same `guava` version. However if the version of `guava` changes, both `b` and
`c` will have to be released and `a` has to be updated to use both new versions, or we have the same problem again.

### Use exclusions

In some cases, we specifically don't want a transitive dependency. For instance, in the above example6 we want
to avoid version the forced dependency version coming from `c`. We can do this by using an exclusion:

```xml
<project>
  <groupId>...</groupId>
  <artifactId>a</artifactId>
  ...
  <dependencies>
    <dependency>
      <groupId>...</groupId>
      <artifactId>b</artifactId>
      <version>...</version>
    </dependency>
    <dependency>
      <groupId>...</groupId>
      <artifactId>c</artifactId>
      <version>...</version>
      <exclusions>
        <exclusion>
          <groupId>com.google.guava</groupId>
          <artifactId>guava</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>16.0</version>
    </dependency>
  </dependencies>
</project>
```

This will have the desired effect of Maven choosing `a`'s direct dependency.

However, exclusions should be used with care as they will remove dependencies from the resolution completely
which means that for instance `maven-dependency-versions-check-plugin` will not see the dependency anymore. This
can become a problem if for instance the dependency on `c` is updated to a newer version of `c` that now
depends on `guava` version `17.0`. If the exclusion is left in place, Maven will never know about this
dependency and thus a bug might have been introduced.
 