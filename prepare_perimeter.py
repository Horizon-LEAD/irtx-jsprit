import geopandas as gpd
import os, argparse

### Command line
parser = argparse.ArgumentParser(description = "Convert OSM perimeter for osmosis")

parser.add_argument("--input-path", type = str, required = True)
parser.add_argument("--output-path", type = str, required = True)

arguments = parser.parse_args()

### Convert file
df = gpd.read_file(arguments.input_path)
df = df.to_crs("EPSG:4326")

df["aggregate"] = 0
area = df.dissolve(by = "aggregate")["geometry"].values[0]

if not hasattr(area, "exterior"):
    print("Selected area is not connected -> Using convex hull.")
    area = area.convex_hull

data = []
data.append("polyfile")
data.append("polygon")

for coordinate in area.exterior.coords:
    data.append("    %e    %e" % coordinate)

data.append("END")
data.append("END")

with open(arguments.output_path, "w+") as f:
    f.write("\n".join(data))
