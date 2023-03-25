# Prereqs

```
export LD_LIBRARY_PATH=<PATH_TO_TENSOR_RT/lib:<PATH_TO_CUDNN>/lib
export CHOCOLATE_PUDDING_ONNX_MODEL_PATH=<PATH TO DOPE ORIGINAL ChocolatePudding.onnx>
```

To create `ChocolatePudding.onnx` follow the instructions from [Quickstart to isaac_ros_pose_estimation](https://github.com/NVIDIA-ISAAC-ROS/isaac_ros_pose_estimation/blob/c6a666d2fb6b3304a71fb7d3d928316fd2ce9510/README.md#quickstart).

# Gathering DOPE original results

`calc_dope_results.py` runs DOPE original decoder for all images in `testset` folder and stores all results to results.json file. This file later used by tests for comparison with `dope4j` results. See [Conformance to DOPE original results](xxxxx) for more details.

Run instructions:

```
git clone https://github.com/NVlabs/Deep_Object_Pose.git
cd Deep_Object_Pose
git checkout 3c407e45e35fee88a218b9c411cc55f08e5b7107
cd docker
docker run --rm --gpus all  -it --privileged --network=host \
 -v FOLDER_WITH_DOPE_MODELS:/tmp/models:rw \
 -v DOPE4J_FOLDER/dope4j.tests:/tmp/dope4j:rw \
 -v /tmp/.X11-unix:/tmp/.X11-unix:rw \
  --env="DISPLAY" \
  --name=nvidia-dope-test nvidia-dope:noetic-v1 bash
python3 /tmp/dope4j/scripts/calc_dope_results.py
```
