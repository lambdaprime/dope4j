# Version 2

- Moving documentation out, updating javadoc, adding test for DeepObjectPoseEstimationService
- Updating to gradle 8.0.2
- Adding Docker support
- Updating to release version of id.opentelemetry-exporters-pack
- Print vertex names
- Fixing error in CacheFileMapper
- Integrating metrics
- Update Cuboid2D to return Point2D points
- Adding conformance results
- Support situations when there are multiple candidates for same cuboid vertex
- Preserve the order of the poses during decoding the network output
- Implementing showResults action
- Adding Dope4jResult and use ObjectMapper instead of JsonMapper to simplify results processing in JSON format
- Modifying calc_dope_results.py to work with processed images
- Process files with calc_dope_results.py in sorted order
- Fixing issues when not all 8 vertices are present
- Return empty list when no poses is found
- Support pose calculation when only 4 vertices available
- Conformance to DOPE original results
- Use LazyInitializer instead of Optional
- Implementing tests by comparing results between DOPE and dope4j
- Providing testsets with results from DOPE
- Support debug mode
- Implementing pose calculation and showProjectedCuboids2D
- Including original camera_info.yaml
- Implementing showCuboids2D and renaming OutputObjects class
- Recalculating peak coordinates with respect to weighted average
- Minor refactoring
- Moving inspector logic to Dope4jInspector
- Adding notion of edges and moving all converter methods to  DjlOpenCvConverters
- Adding cameraInfo support
- Adding primitive types for Cuboid and Point
- Moving application logic to separate package
- Fixes in README
- Decoding detected objects and implementing showMatchedVertices command
- Allow users to change threshold
- Implementing options recursiveScan, imageFileRegexp and don't show images by default
- Implementing new options: cache, totalRunTime
- Adding showAffinityFields
- Implementing showVerticesBeliefs and showCenterPointBeliefs
- Implementing launcher script
- Use addDependencies in gradle scripts
- Minor improvements
- Adding initial implementation
- Initial commit

[dope4j-2.0.jar](https://github.com/lambdaprime/dope4j/raw/main/dope4j/release/dope4j-2.0.jar)

[dope4j-app-v2.0.zip](https://github.com/lambdaprime/dope4j/raw/main/dope4j.app/release/dope4j-app-v2.0.zip)
