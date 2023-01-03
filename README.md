**dope4j** - provides access to [DOPE](https://github.com/NVlabs/Deep_Object_Pose) models from Java. Based on [Deep Java Library](https://djl.ai/)

Project is not ready yet.

# Requirements

- Java 17+
- CUDA 11.7
- CUDNN 8.7.0
- TensorRT 8.4.3.1

All libraries should be available globally in the system or provided separately as follows:

```
export LD_LIBRARY_PATH=<PATH_TO_TENSOR_RT/lib:<PATH_TO_CUDNN>/lib
```

# Documentation

[Development](DEVELOPMENT.md)

# Usage

```
dope4j <options>
```

Required options:

`-modelUrl=<path>` - path to [DOPE](https://github.com/NVlabs/Deep_Object_Pose) model in ONNX format.

`-imagePath=<path>` - path to input image file or folder. In case of folder `dope4j` will search it for image files

Optional options:

`-showVerticesBeliefs=<true|false>` - for each image show all 8 vertices detected during inference step. They all part of Belief Maps. There is one Belief Map per vertex. Default "false".

`-showCenterPointBeliefs=<true|false>` - for each image show all N center points detected during inference step. They all part of single Belief Map. Default "false".

`-showAffinityFields=<true|false>` - for each image show all Affinity Fields detected for all 8 vertices during inference step. Every Affinity Field is a vector field where each vector points towards center point. Default "false".

`-cache=<true|false>` - enable caching of preprocessed input images and output tensors. Default "false".

`-cacheFolder=<path>` - file system location where cache will be stored. Default is "_cache_dope4j" inside system temporary folder

`-totalRunTime=<true|false>` - print command total execution time when command finishes.

# Contributors

lambdaprime <intid@protonmail.com>
