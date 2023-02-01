**dope4j** - provides access to [Deep Object Pose Estimation (DOPE)](https://github.com/NVlabs/Deep_Object_Pose) models from Java. Based on [Deep Java Library](https://djl.ai/)

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

[Conformance to DOPE original results](dope4j.tests/Conformance_to_DOPE.md)

[Development](DEVELOPMENT.md)

# Usage

```
dope4j <options>
```

Required options:

`-modelUrl=<path>` - path to [DOPE](https://github.com/NVlabs/Deep_Object_Pose) model in ONNX format. Each model can detect single object. Where to download models and how to covert them to ONNX can be found in [Quickstart to isaac_ros_pose_estimation](https://github.com/NVIDIA-ISAAC-ROS/isaac_ros_pose_estimation/blob/c6a666d2fb6b3304a71fb7d3d928316fd2ce9510/README.md#quickstart). 

`-objectSize=<width>,<height>,<length>` - size of object's 3D cuboid which DOPE model is trained to detect. See [list of DOPE original object sizes](config/dimensions.md)

`-imagePath=<path>` - path to input image file or folder. In case of folder `dope4j` will search it for image files

`-cameraInfo=<path>` - path to file with camera information in [camera_calibration_parsers](http://wiki.ros.org/camera_calibration_parsers#YAML) format. The images should be rectified before feeding them to DOPE. It means that if you want to use your camera you need to calibrate it first. This can be done using `camera_calibration_parsers` as described in [DOPE Camera Tutorial](https://github.com/NVlabs/Deep_Object_Pose/blob/3c407e45e35fee88a218b9c411cc55f08e5b7107/doc/camera_tutorial.md) For testing with DOPE original datasets use [original camera_info.yaml](https://github.com/NVlabs/Deep_Object_Pose/blob/3c407e45e35fee88a218b9c411cc55f08e5b7107/config/camera_info.yaml) (copy of it can be found inside `config` folder).

Optional options:

`-showVerticesBeliefs=<true|false>` - for each image show all candidate vertices detected during inference step. Candidate vertices belong to one of the 8 vertices of cuboid which surrounds the object. They all part of Belief Maps. There is one Belief Map per each vertex of the cuboid. Default is "false".

`-showCenterPointBeliefs=<true|false>` - for each image show all center points detected during inference step. They all part of single Belief Map. Default is "false".

`-showAffinityFields=<true|false>` - for each image show all Affinity Fields detected during inference step. There are Affinity Fields for all 8 vertices of cuboid which surrounds the object. Every Affinity Field is a vector field where each vector points towards center point of the cuboid. Default is "false".

`-showMatchedVertices=<true|false>` - for each vertex show with which center point it matches. Default is "false".

`-showCuboids2D=<true|false>` - for each detected object show a cuboid. The cuboid is rendered directly on the vertices which are returned by the DOPE network (no projection is done). Default is "false".

`-showProjectedCuboids2D=<true|false>` - for each detected object show a projection of its cuboid 3D model. Default is "false".

`-cache=<true|false>` - enable caching of preprocessed input images and output tensors. Default is "false".

`-cacheFolder=<path>` - file system location where cache will be stored. Default is "_cache_dope4j" inside system temporary folder.

`-recursiveScan=<true|false>` - when `-imagePath` points to a folder then this option will control if `dope4j` will look for images in subfolders or not. Default is "false".

`-imageFileRegexp=<regexp>` - when `-imagePath` points to a folder then this regular expression will be used to filter images. Default is ".*\.(png|jpg)" which means that only "png" and "jpg" images will be used to run inference.

`-threshold=<double>` - set threshold value which is used to filter the keypoints. Default is "0.01".

`-debug=<true|false>` - print debug information and log it to `dope4j-debug.log` inside system temporary folder. Default is "false".

`-totalRunTime=<true|false>` - print total execution time when command finishes.

# Contributors

lambdaprime <intid@protonmail.com>
