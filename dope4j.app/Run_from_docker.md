`dope4j` CLI application can be run inside Docker. That way installing TensorRT in not required as `dope4j` image comes with it.

To build `dope4j` image:
```
docker build -t dope4j .
```

Run:
```
docker run --gpus all -it \
 -v MAVEN_HOME:/home/ubuntu/.m2 \
 -v MODELS_FOLDER:/tmp/models \
 -v DOPE4J_PROJECT/config:/tmp/dope4j/config \
 -v DATASET_FOLDER:/tmp/dataset \
 -e DISPLAY=$DISPLAY \
 -v /tmp/.X11-unix:/tmp/.X11-unix  dope4j
```

Where:

`MAVEN_HOME` - usually it is located at home folder ~/.m2. It is not required but it helps to avoid downloading every time some of the [Deep Java Library](https://djl.ai/) dependencies.

`DOPE4J_PROJECT` - source repository location.

`DATASET_FOLDER` - folder with images
