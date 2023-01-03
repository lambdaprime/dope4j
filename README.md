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

# Usage

```
dope4j <options>
```

Available options:

`-modelUrl=<path>` - path to [DOPE](https://github.com/NVlabs/Deep_Object_Pose) model in ONNX format.

`-imagePath=<path>` - path to input image file or folder. In case of folder `dope4j` will search it for image files

`-showVerticesBeliefs=<true|false>` - for each image show all vertices detected during inference step. They all part of Belief Maps. There is one Belief Map per vertex.

`-showCenterPointBeliefs=<true|false>` - for each image show all center points detected during inference step. They all part of single Belief Map.

# Documentation

[Development](DEVELOPMENT.md)

# Contributors

lambdaprime <intid@protonmail.com>
