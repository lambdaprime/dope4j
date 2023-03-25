**dope4j** - provides access to [Deep Object Pose Estimation (DOPE)](https://github.com/NVlabs/Deep_Object_Pose) models from Java. Based on [Deep Java Library](https://djl.ai/)

This project consist from:

`dope4j` - Java library which can be used to run inference with DOPE models and detect object poses.

`dope4j-app` - Java application which exposes `dope4j` library functionality through CLI.

# Download

[Release versions](/CHANGELOG.md)

Or you can add dependency to it as follows:

Gradle:

```
dependencies {
  implementation 'io.github.lambdaprime:dope4j:2.0'
}
```

# Documentation

[Documentation](http://portal2.atwebpages.com/dope4j)

[Development](DEVELOPMENT.md)

# Usage

```
dope4j-app -action=<runInference|showResults> <options>
```

## runInference

Options:
```
-modelUrl=<path>
-objectSize=<width>,<height>,<length>
-imagePath=<path>
-cameraInfo=<path>
-showVerticesBeliefs=<true|false>
-showCenterPointBeliefs=<true|false>
-showAffinityFields=<true|false>
-showMatchedVertices=<true|false>
-showCuboids2D=<true|false>
-showProjectedCuboids2D=<true|false>
-cache=<true|false>
-cacheFolder=<path>
-recursiveScan=<true|false>
-imageFileRegexp=<regexp>
-threshold=<double>
-debug=<true|false>
-exportMetricsToCsv=<path>
-exportMetricsToElastic=<elasticsearch_url>
-totalRunTime=<true|false>
-lineThickness=<int>
```

## showResults

Options:
```
-resultsJson=<path>
-imagesRoot=<path>
```

# Contributors

lambdaprime <intid@protonmail.com>
