# See README.md for more details
#
# Website: https://github.com/lambdaprime/dope4j
#

from dope.inference.detector import ModelData, ObjectDetector
from dope.inference.cuboid import Cuboid3d
from dope.inference.cuboid_pnp_solver import CuboidPNPSolver
import cv2
import os
import numpy as np
import json

def printImg(img):
    for x in range(5):
        for y in range(5):
            for c in range(3):
                print(img.item(x, y, c))

def fmt(num):
    return num #round(num, 3)

def point2d(ar):
    return {
        "x": fmt(ar[0]),
        "y": fmt(ar[1])
    }

def point3d(ar):
    return {
        "x": fmt(ar[0]),
        "y": fmt(ar[1]),
        "z": fmt(ar[2])
    }

print("Starting")

DEBUG=False

config_detect = lambda: None
config_detect.mask_edges = 1
config_detect.mask_faces = 1
config_detect.vertex = 1
config_detect.threshold = 0.5
config_detect.softmax = 1000
config_detect.thresh_angle = 0.5
config_detect.thresh_map = 0.01
config_detect.sigma = 3
config_detect.thresh_points = 0.1

camera_matrix = np.matrix([641.5, 0, 320.0, 0, 641.5, 240.0, 0, 0, 1], dtype='float64').reshape(3, 3)
if DEBUG is True:
    print("camera_matrix")
    print(camera_matrix)

dist_coeffs = np.zeros((5, 1))
if DEBUG is True:
    print("dist_coeffs")
    print(dist_coeffs)

print("Loading ChocolatePudding model")
model = ModelData("ChocolatePudding", "/tmp/models/ChocolatePudding.pth")
model.load_net_model()

pnp_solver = \
                CuboidPNPSolver(
                    model,
                    cuboid3d=Cuboid3d([4.947199821472168, 2.9923000335693359, 8.3498001098632812])
                )
pnp_solver.set_camera_intrinsic_matrix(camera_matrix)
pnp_solver.set_dist_coeffs(dist_coeffs)

outFile = open('/tmp/dope4j/testset/results.json', 'w')

# use processed images which sent to the network directly
testsetDir = "/tmp/dope4j/testset/_cache"

for filePath in sorted(os.listdir(testsetDir)):
    if not filePath.endswith(".png"):
        continue
    img = cv2.imread(testsetDir + "/" + filePath)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    if DEBUG is True:
        print("img")
        print(type(img))
        print(img.shape)
        printImg(img)
    
    results, im_belief = ObjectDetector.detect_object_in_image(
        model.net,
        pnp_solver,
        img,
        config_detect)

    objects2d = []
    poses = []
    for result in results:
        objects2d.append({
            "center": point2d(result['projected_points'][8]),
            "vertices": [
                point2d(result['projected_points'][0]),
                point2d(result['projected_points'][1]),
                point2d(result['projected_points'][2]),
                point2d(result['projected_points'][3]),
                point2d(result['projected_points'][4]),
                point2d(result['projected_points'][5]),
                point2d(result['projected_points'][6]),
                point2d(result['projected_points'][7])
            ]
        })
        if result['location'] is not None:
            poses.append({
                "position": point3d(result['location']),
                #"orientation": {
                #    "values": [fmt(n) for n in np.array(result['quaternion']).tolist()]
                #}
            })
    outJson = json.dumps({
        "imagePath": filePath,
        "detectedPoses": {
            'objects2d': objects2d,
            "poses": poses
        }
    }, indent=4)
    print(outJson)
    outFile.write(outJson)

outFile.close()
