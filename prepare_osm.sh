set -e

input_path="$1"
perimeter_path="$2"
output_path="$3"

osmosis --read-pbf ${input_path} \
  --tf accept-ways highway=* \
  --bounding-polygon file=${perimeter_path} completeWays=true \
  --used-node \
  --write-pbf ${output_path}
